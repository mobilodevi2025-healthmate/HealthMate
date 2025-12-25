package com.mobil.healthmate.data.local.types

enum class MealType(val displayName: String) {
    BREAKFAST("Kahvaltı"),
    LUNCH("Öğle Yemeği"),
    DINNER("Akşam Yemeği"),
    SNACK("Ara Öğün");
}

enum class FoodUnit(val displayName: String) {
    PIECE("Adet"),
    PORTION("Porsiyon"),
    GRAM("Gram"),
    ML("Mililitre"),
    SLICE("Dilim"),
    TABLE_SPOON("Yemek Kaşığı"),
    GLASS("Su Bardağı");
}

enum class Gender(val displayName: String) {
    MALE("Erkek"),
    FEMALE("Kadın")
}

enum class ActivityLevel(val factor: Double, val displayName: String) {
    SEDENTARY(1.2, "Hareketsiz"),
    LIGHTLY_ACTIVE(1.375, "Az Hareketli"),
    MODERATELY_ACTIVE(1.55, "Orta Hareketli"),
    VERY_ACTIVE(1.725, "Çok Hareketli")
}

enum class GoalType(val displayName: String) {
    LOSE_WEIGHT("Kilo Vermek"),
    GAIN_WEIGHT("Kilo Almak"),
    MAINTAIN_WEIGHT("Kiloyu Korumak"),
    MUSCLE_BUILD("Kas Yapmak")
}