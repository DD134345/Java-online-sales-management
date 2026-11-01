package freshlink.database.model;

/** A saved delivery address of a customer. */
public record Address(int addressId, int userId, String receiverName, String phone,
                      String street, String ward, String district, String city, boolean isDefault) {

    /** Full address on one line, e.g. "25 Nguyen Thi Thap, Phuong Tan Phu, Quan 7, TP. Ho Chi Minh". */
    public String fullAddress() {
        return street + ", " + ward + ", " + district + ", " + city;
    }
}
