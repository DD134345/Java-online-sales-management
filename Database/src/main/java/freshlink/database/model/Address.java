package freshlink.database.model;

public record Address(int addressId, int userId, String receiverName, String phone,
                      String street, String ward, String district, String city, boolean isDefault) {

    // e.g. "25 Nguyễn Thị Thập, Phường Tân Phú, Quận 7, TP. Hồ Chí Minh"
    public String fullAddress() {
        return street + ", " + ward + ", " + district + ", " + city;
    }
}
