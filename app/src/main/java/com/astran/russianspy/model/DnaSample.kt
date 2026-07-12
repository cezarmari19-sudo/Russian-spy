package com.astran.russianspy.model

data class DnaSample(
    val id: String,
    val roomId: String,
    val actualOwnerId: String,      // cui apartine cu adevarat ADN-ul
    var displayedOwnerId: String,    // ce arata analiza (poate fi schimbat de spion)
    var completeness: Int,           // 10-100, procent de cat de complet e
    var isAnalyzed: Boolean = false,
    var wasTamperedWith: Boolean = false
) {
    fun isReliable(): Boolean = completeness >= 70
}
