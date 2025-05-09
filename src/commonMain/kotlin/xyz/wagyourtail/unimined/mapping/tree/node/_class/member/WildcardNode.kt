package xyz.wagyourtail.unimined.mapping.tree.node._class.member

import xyz.wagyourtail.unimined.mapping.Namespace
import xyz.wagyourtail.unimined.mapping.formats.umf.UMFWriter
import xyz.wagyourtail.unimined.mapping.jvms.ext.FieldOrMethodDescriptor
import xyz.wagyourtail.unimined.mapping.jvms.four.ElementType
import xyz.wagyourtail.unimined.mapping.jvms.four.two.one.InternalName
import xyz.wagyourtail.unimined.mapping.tree.node.LazyResolvableEntry
import xyz.wagyourtail.unimined.mapping.tree.node.LazyResolvables
import xyz.wagyourtail.unimined.mapping.tree.node.SignatureNode
import xyz.wagyourtail.unimined.mapping.tree.node._class.ClassNode
import xyz.wagyourtail.unimined.mapping.tree.node._class.member.method.ExceptionNode
import xyz.wagyourtail.unimined.mapping.tree.node._class.member.method.LocalNode
import xyz.wagyourtail.unimined.mapping.tree.node._class.member.method.ParameterNode
import xyz.wagyourtail.unimined.mapping.visitor.*
import xyz.wagyourtail.unimined.mapping.visitor.delegate.DelegateWildcardVisitor

class WildcardNode(parent: ClassNode, val type: WildcardType, descs: Map<Namespace, FieldOrMethodDescriptor>) : MemberNode<WildcardVisitor, ClassVisitor>(parent), WildcardVisitor, LazyResolvableEntry<WildcardNode, WildcardVisitor> {
    private val _descs = descs.toMutableMap()
    private val _signatures: MutableSet<SignatureNode<WildcardVisitor>> = mutableSetOf()
    private val _locals: MutableList<LocalNode<WildcardVisitor>> = mutableListOf()
    private val _exceptions: MutableList<ExceptionNode<WildcardVisitor>> = mutableListOf()

    val descs: Map<Namespace, FieldOrMethodDescriptor> get() = _descs
    val signatures: Set<SignatureNode<WildcardVisitor>> get() = _signatures
    val params = LazyResolvables<ParameterVisitor, ParameterNode<WildcardVisitor>>(root)
    val locals: List<LocalNode<WildcardVisitor>> get() = _locals
    val exceptions: List<ExceptionNode<WildcardVisitor>> get() = _exceptions

    fun hasDescriptor() = descs.isNotEmpty()

    fun getDescriptor(namespace: Namespace): FieldOrMethodDescriptor? {
        if (descs.isEmpty()) return null
        if (namespace in descs) {
            return descs[namespace]
        }
        val fromNs = descs.keys.first()
        return root.map(fromNs, namespace, descs[fromNs]!!)
    }

    fun setDescriptors(descs: Map<Namespace, FieldOrMethodDescriptor>) {
        root.mergeNs(descs.keys)
        this._descs.putAll(descs)
    }

    fun getMethodDescriptor(namespace: Namespace) = getDescriptor(namespace)?.getMethodDescriptor()

    fun getFieldDescriptor(namespace: Namespace) = getDescriptor(namespace)?.getFieldDescriptor()


    override fun visitSignature(value: String, baseNs: Namespace, namespaces: Set<Namespace>): SignatureVisitor {
        val node = SignatureNode(this, value, baseNs)
        node.addNamespaces(namespaces)
        _signatures.add(node)
        return node
    }

    override fun visitParameter(index: Int?, lvOrd: Int?, names: Map<Namespace, String>): ParameterVisitor? {
        if (type == WildcardType.FIELD) return null
        val newParam = ParameterNode(this, index, lvOrd)
        newParam.setNames(names)
        params.addUnresolved(newParam)
        return newParam
    }

    override fun visitLocalVariable(lvOrd: Int, startOp: Int?, names: Map<Namespace, String>): LocalVariableVisitor? {
        if (type == WildcardType.FIELD) return null
        for (local in locals) {
            if (lvOrd == local.lvOrd && startOp == local.startOp) {
                local.setNames(names)
                return local
            }
        }
        val newLocal = LocalNode(this, lvOrd, startOp)
        newLocal.setNames(names)
        _locals.add(newLocal)
        return newLocal
    }

    override fun visitException(
        type: ExceptionType,
        exception: InternalName,
        baseNs: Namespace,
        namespaces: Set<Namespace>
    ): ExceptionVisitor? {
        if (this.type == WildcardType.FIELD) return null
        val node = ExceptionNode(this, type, exception, baseNs)
        node.addNamespaces(namespaces)
        _exceptions.add(node)
        return node
    }
    override fun acceptOuter(visitor: ClassVisitor, nsFilter: Collection<Namespace>): WildcardVisitor? {
        if (descs.isEmpty()) return visitor.visitWildcard(type, emptyMap())
        return visitor.visitWildcard(type, nsFilter.associateWith { getDescriptor(it)!! })
    }

    override fun acceptInner(visitor: WildcardVisitor, nsFilter: Collection<Namespace>, sort: Boolean) {
        super.acceptInner(visitor, nsFilter, sort)
        for (signature in if (sort) signatures.sortedBy { it.toString() } else signatures) {
            signature.accept(visitor, nsFilter, sort)
        }
        for (exception in if (sort) exceptions.sortedBy { it.toString() } else exceptions) {
            exception.accept(visitor, nsFilter, sort)
        }
        for (param in if (sort) params.resolve().sortedBy { it.toString() } else params.resolve()) {
            if (param.names.isEmpty()) continue
            param.accept(visitor, nsFilter, sort)
        }
        for (local in if (sort) locals.sortedBy { it.toString() } else locals) {
            local.accept(visitor, nsFilter, sort)
        }
    }

    enum class WildcardType {
        METHOD,
        FIELD
        ;

        fun asElementType(): ElementType {
            return when (this) {
                METHOD -> ElementType.METHOD
                FIELD -> ElementType.FIELD
            }
        }
    }

    fun doMerge(target: WildcardNode) {
        acceptInner(target, root.namespaces, false)
    }

    override fun merge(element: WildcardNode): Boolean {
        if (element.type != type) return false
        if (element.descs.isEmpty() && descs.isEmpty()) {
            doMerge(element)
            return true
        }
        if (element.descs.isNotEmpty() && descs.isNotEmpty()) {
            val descKey = element.descs.keys.intersect(descs.keys).firstOrNull() ?: descs.keys.first()
            if (element.getDescriptor(descKey) == descs[descKey]) {
                element.setDescriptors(descs)
                doMerge(element)
                return true
            }
        }
        return false
    }

    override fun toUMF(inner: Boolean) = buildString {
        val delegator = UMFWriter.UMFWriterDelegator(::append, true)
        delegator.namespaces = root.namespaces
        delegator.visitWildcard(EmptyClassVisitor(), type, descs)
        if (inner) acceptInner(DelegateWildcardVisitor(EmptyWildcardVisitor(), delegator), root.namespaces, true)
    }

}