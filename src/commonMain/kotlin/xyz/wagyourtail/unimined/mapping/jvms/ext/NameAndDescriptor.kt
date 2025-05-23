package xyz.wagyourtail.unimined.mapping.jvms.ext

import xyz.wagyourtail.unimined.mapping.jvms.JVMS
import xyz.wagyourtail.unimined.mapping.jvms.TypeCompanion
import xyz.wagyourtail.unimined.mapping.jvms.four.two.two.UnqualifiedName
import xyz.wagyourtail.commonskt.reader.CharReader
import xyz.wagyourtail.unimined.mapping.jvms.Type
import kotlin.jvm.JvmInline

/**
 * NameAndDescriptor
 *  [UnqualifiedName] [; [FieldOrMethodDescriptor]]
 */
@JvmInline
value class NameAndDescriptor(val value: String) : Type {

    constructor(name: UnqualifiedName, descriptor: FieldOrMethodDescriptor?) : this(buildString {
        append(name)
        if (descriptor != null) {
            append(';')
            append(descriptor)
        }
    })

    companion object : TypeCompanion<NameAndDescriptor> {
        override fun shouldRead(reader: CharReader<*>): Boolean {
            return reader.take() !in JVMS.unqualifiedNameIllegalChars
        }

        override fun read(reader: CharReader<*>, append: (Any) -> Unit) {
            append(UnqualifiedName.read(reader))
            if (!reader.exhausted() && reader.peek() == ';') {
                append(reader.expect(';'))
                append(FieldOrMethodDescriptor.read(reader))
            }
        }

        override fun unchecked(value: String) = NameAndDescriptor(value)

    }

    fun getParts(): Pair<UnqualifiedName, FieldOrMethodDescriptor?> {
        val name = value.substringBefore(';')
        val desc = if (';' in value) {
            FieldOrMethodDescriptor.unchecked(value.substringAfter(';'))
        } else {
            null
        }
        return UnqualifiedName.unchecked(name) to desc
    }

    override fun accept(visitor: (Any) -> Boolean) {
        if (visitor(this)) {
            val (name, desc) = getParts()
            name.accept(visitor)
            desc?.accept(visitor)
        }
    }

    override fun toString() = value



}