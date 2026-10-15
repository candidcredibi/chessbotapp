package com.example.engine

enum class EngineStyle(
    val titleRu: String,
    val descriptionRu: String,
    val maxAcceptableCentipawnLoss: Int
) {
    SAFE(
        titleRu = "Безопасный",
        descriptionRu = "Приоритет надежной защите, сохранению материала и минимизации рисков.",
        maxAcceptableCentipawnLoss = 30
    ),
    AGGRESSIVE(
        titleRu = "Агрессивный",
        descriptionRu = "Поиск активных атак на короля, форсированных шахов и острых разменов.",
        maxAcceptableCentipawnLoss = 60
    ),
    POSITIONAL(
        titleRu = "Позиционный",
        descriptionRu = "Улучшение расположения фигур, контроль центра и гармоничная структура пешек.",
        maxAcceptableCentipawnLoss = 40
    ),
    TACTICAL(
        titleRu = "Тактический",
        descriptionRu = "Выявление вилок, связок, вскрытых нападений и форсированных комбинаций.",
        maxAcceptableCentipawnLoss = 50
    );

    companion object {
        fun fromName(name: String): EngineStyle = runCatching {
            valueOf(name)
        }.getOrDefault(SAFE)
    }
}
