package xyz.wagyourtail.unimined.mapping.jvms.four.seven.nine.one.reference

import xyz.wagyourtail.unimined.mapping.jvms.TypeCompanion
import xyz.wagyourtail.unimined.mapping.jvms.four.seven.nine.one.JavaTypeSignature
import xyz.wagyourtail.commonskt.reader.CharReader
import xyz.wagyourtail.unimined.mapping.jvms.Type
import kotlin.jvm.JvmInline

/**
 * ArrayTypeSignature:
 *   [ [JavaTypeSignature]
 */
@JvmInline
value class ArrayTypeSignature private constructor(val value: String) : Type {

    companion object: TypeCompanion<ArrayTypeSignature> {

        override fun shouldRead(reader: CharReader<*>): Boolean {
            return reader.take() == '['
        }

        override fun read(reader: CharReader<*>, append: (Any) -> Unit) {
            append(reader.expect('['))
            append(JavaTypeSignature.read(reader))
        }

        override fun unchecked(value: String) = ArrayTypeSignature(value)
    }

    fun getParts(): JavaTypeSignature {
        return JavaTypeSignature.unchecked(value.substring(1))
    }

    override fun accept(visitor: (Any) -> Boolean) {
        if (visitor(this)) {
            visitor("[")
            getParts().accept(visitor)
        }
    }

    override fun toString() = value

}
