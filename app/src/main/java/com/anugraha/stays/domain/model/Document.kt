package com.anugraha.stays.domain.model

data class Document(
    val id: String,
    val name: String,
    val url: String,
    val uploadedAt: Long,
    val type: DocumentType = DocumentType.ID_PROOF
)

enum class DocumentType {
    ID_PROOF,
    PASSPORT,
    DRIVING_LICENSE,
    OTHER
}