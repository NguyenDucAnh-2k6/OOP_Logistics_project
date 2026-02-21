package com.oop.logistics.preprocessing;

import java.util.*;

/**
 * Extracts location information from text (Static Utility)
 */
public class LocationExtractor {
    
    // Static final fields initialized exactly once
    private static final Set<String> vietnameseProvinces = initializeProvinces();
    private static final Set<String> vietnameseCities = initializeCities();
    
    // Private constructor prevents anyone from creating an object of this utility class
    private LocationExtractor() {}
    
    /**
     * Extract all possible locations from text, case-insensitive and deduplicated.
     */
    public static List<String> extractAllLocations(String text) {
        // Use a Set to prevent double-counting if an article triggers both the province and city lists
        Set<String> uniqueLocations = new HashSet<>();
        
        if (text == null || text.isEmpty()) return new ArrayList<>();
        
        // Convert text to lowercase once for faster, case-insensitive searching
        String lowerText = text.toLowerCase();
        
        for (String province : vietnameseProvinces) {
            if (lowerText.contains(province.toLowerCase())) {
                uniqueLocations.add(capitalizeWords(province));
            }
        }
        for (String city : vietnameseCities) {
            if (lowerText.contains(city.toLowerCase())) {
                uniqueLocations.add(capitalizeWords(city));
            }
        }
        
        return new ArrayList<>(uniqueLocations);
    }
    
    /**
     * Helper to ensure all locations are formatted beautifully for the UI (e.g. "bắc ninh" -> "Bắc Ninh")
     */
    private static String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) return "";
        String[] words = text.split("\\s+");
        StringBuilder capitalized = new StringBuilder();
        
        for (String word : words) {
            if (word.length() > 0) {
                capitalized.append(Character.toUpperCase(word.charAt(0)))
                          .append(word.substring(1).toLowerCase())
                          .append(" ");
            }
        }
        return capitalized.toString().trim();
    }
    
    /**
     * Add custom location to the recognizer
     */
    public static void addProvince(String province) {
        if (province != null && !province.trim().isEmpty()) {
            vietnameseProvinces.add(province.trim());
        }
    }
    
    public static void addCity(String city) {
        if (city != null && !city.trim().isEmpty()) {
            vietnameseCities.add(city.trim());
        }
    }
    
    private static Set<String> initializeProvinces() {
        return new HashSet<>(Arrays.asList(
            "Hà Nội", "Hồ Chí Minh", "Đà Nẵng", "Hải Phòng", "Cần Thơ",
            "An Giang", "Bà Rịa-Vũng Tàu", "Bắc Giang", "Bắc Kạn", "Bạc Liêu",
            "Bắc Ninh", "Bến Tre", "Bình Định", "Bình Dương", "Bình Phước",
            "Bình Thuận", "Cà Mau", "Cao Bằng", "Đắk Lắk", "Đắk Nông", "Điện Biên",
            "Đồng Nai", "Đồng Tháp", "Gia Lai", "Hà Giang", "Hà Nam", "Hà Tĩnh",
            "Hải Dương", "Hậu Giang", "Hòa Bình", "Hưng Yên", "Khánh Hòa",
            "Kiên Giang", "Kon Tum", "Lai Châu", "Lâm Đồng", "Lạng Sơn", "Lào Cai",
            "Long An", "Nam Định", "Nghệ An", "Ninh Bình", "Ninh Thuận", "Phú Thọ",
            "Phú Yên", "Quảng Bình", "Quảng Nam", "Quảng Ngãi", "Quảng Ninh",
            "Quảng Trị", "Sóc Trăng", "Sơn La", "Tây Ninh", "Thái Bình", "Thái Nguyên",
            "Thanh Hóa", "Thừa Thiên Huế", "Tiền Giang", "Trà Vinh", "Tuyên Quang",
            "Vĩnh Long", "Vĩnh Phúc", "Yên Bái"
        ));
    }
    
    private static Set<String> initializeCities() {
        return new HashSet<>(Arrays.asList(
            "thủ đức", "hạ long", "cẩm phả", "uông bí", "móng cái", 
            "thái nguyên", "sông công", "phổ yên", "việt trì", "vĩnh yên", 
            "phúc yên", "bắc ninh", "từ sơn", "hải dương", "chí linh", 
            "hưng yên", "thái bình", "nam định", "ninh bình", "tam điệp",
            "thanh hóa", "sầm sơn", "bỉm sơn", "vinh", "cửa lò", "thái hòa", 
            "hà tĩnh", "đồng hới", "đông hà", "huế", "hội an", "tam kỳ", 
            "quảng ngãi", "quy nhơn", "tuy hòa", "nha trang", "cam ranh", 
            "phan rang", "phan thiết", "biên hòa", "long khánh", "vũng tàu", 
            "bà rịa", "thủ dầu một", "dĩ an", "thuận an", "tân uyên", 
            "bến cát", "đồng xoài", "tây ninh", "mỹ tho", "bến tre", 
            "trà vinh", "vĩnh long", "sa đéc", "cao lãnh", "hồng ngự", 
            "long xuyên", "châu đốc", "rạch giá", "hà tiên", "vị thanh", 
            "ngã bảy", "sóc trăng", "bạc liêu", "cà mau", "buôn ma thuột", 
            "pleiku", "kon tum", "gia nghĩa", "đà lạt", "bảo lộc"
        ));
    }
}