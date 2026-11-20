package com.hostelos.mess;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Starting menus the mess rotates through. These are only seeds for new meal rows: the price of a
 * meal lives in the database (meals.price) and can be changed by the menu manager at any time.
 */
final class MenuCatalog {
    record Preset(String items, String price, String imageUrl) {
        BigDecimal priceValue() { return new BigDecimal(price); }
    }

    private MenuCatalog() {}

    static final Map<MealCategory, List<Preset>> PRESETS = Map.of(
        MealCategory.BREAKFAST, List.of(
            new Preset("Poha, Jalebi, Masala chai", "40.00", "/food/poha.jpg"),
            new Preset("Idli, Sambar, Coconut chutney", "45.00", "/food/idli.jpg"),
            new Preset("Aloo paratha, Curd, Pickle", "50.00", "/food/paratha.jpg"),
            new Preset("Bread, Omelette, Banana", "45.00", "/food/omelette.jpg")),
        MealCategory.LUNCH, List.of(
            new Preset("Steamed rice, Dal tadka, Roti, Paneer butter masala, Seasonal sabzi", "90.00", "/food/thali.jpg"),
            new Preset("Jeera rice, Rajma, Roti, Mixed veg, Salad", "80.00", "/food/rajma.jpg"),
            new Preset("Steamed rice, Sambar, Roti, Aloo gobi, Papad", "75.00", "/food/thali.jpg"),
            new Preset("Veg pulao, Chole, Roti, Raita, Salad", "85.00", "/food/chole.jpg")),
        MealCategory.SNACKS, List.of(
            new Preset("Samosa, Green chutney, Tea", "25.00", "/food/samosa.jpg"),
            new Preset("Veg sandwich, Tea", "30.00", "/food/sandwich.jpg"),
            new Preset("Pav bhaji, Lemon water", "40.00", "/food/pavbhaji.jpg")),
        MealCategory.DINNER, List.of(
            new Preset("Roti, Steamed rice, Dal fry, Mixed vegetables, Paneer bhurji", "85.00", "/food/thali.jpg"),
            new Preset("Roti, Veg biryani, Raita, Dal makhani", "90.00", "/food/biryani.jpg"),
            new Preset("Roti, Rice, Kadhi, Bhindi masala, Gulab jamun", "80.00", "/food/thali.jpg"),
            new Preset("Roti, Rice, Dal, Matar paneer, Salad", "85.00", "/food/thali.jpg")));
}
