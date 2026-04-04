package com.dayscounter.domain.usecase

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Сериализатор для BackupFormat.
 *
 * Сериализует enum в lowercase строки ("android", "ios") для совместимости с iOS.
 * Десериализует оба формата: lowercase и uppercase.
 */
object BackupFormatSerializer : KSerializer<BackupFormat> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BackupFormat", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: BackupFormat
    ) {
        encoder.encodeString(value.serialName)
    }

    override fun deserialize(decoder: Decoder): BackupFormat {
        val string = decoder.decodeString()
        return BackupFormat.entries.find {
            it.serialName == string ||
                it.name.equals(
                    string,
                    ignoreCase = true
                )
        }
            ?: throw IllegalArgumentException("Unknown BackupFormat: $string")
    }

    private val BackupFormat.serialName: String
        get() =
            when (this) {
                BackupFormat.ANDROID -> "android"
                BackupFormat.IOS -> "ios"
            }
}
