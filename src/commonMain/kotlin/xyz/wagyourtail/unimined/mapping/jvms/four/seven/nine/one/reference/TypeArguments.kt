package xyz.wagyourtail.unimined.mapping.jvms.four.seven.nine.one.reference

import xyz.wagyourtail.unimined.mapping.jvms.TypeCompanion
import xyz.wagyourtail.commonskt.reader.CharReader
import xyz.wagyourtail.commonskt.reader.StringCharReader
import xyz.wagyourtail.unimined.mapping.jvms.Type
import kotlin.jvm.JvmInline

/**
 * TypeArguments:
 *   < [TypeArgument] {[TypeArgument]} >
 */
@JvmInline
value class TypeArguments private constructor(val value: String) : Type {

    companion object: TypeCompanion<TypeArguments> {
        override fun shouldRead(reader: CharReader<*>): Boolean {
            return reader.take() == '<'
        }

        override fun read(reader: CharReader<*>, append: (Any) -> Unit) {
            append(reader.expect('<'))
            while (reader.peek() != '>') {
                append(TypeArgument.read(reader))
            }
            append(reader.expect('>'))
        }

        override fun unchecked(value: String) = TypeArguments(value)
    }

    fun getParts(): List<TypeArgument> = StringCharReader(value.substring(1, value.length - 1)).let {
        val args = mutableListOf<TypeArgument>()
        while (true) {
            args.add(TypeArgument.read(it))
            if (it.exhausted()) {
                break
            }
        }
        return args
    }

    override fun accept(visitor: (Any) -> Boolean) {
        if (visitor(this)) {
            visitor("<")
            getParts().forEach { it.accept(visitor) }
            visitor(">")
        }
    }

    override fun toString() = value

}