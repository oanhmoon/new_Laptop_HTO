package org.example.laptopstore.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.example.laptopstore.util.Constant.*;

@Getter
public enum ErrorCode {

    ERROR_CODE_NOT_FOUND(NOT_FOUND, NO_CONTENT_MESSAGE, HttpStatus.NOT_FOUND,
            "Không tìm thấy mã lỗi tương ứng trong hệ thống"),

    ERROR_LIST_EMPTY(BAD_REQUEST, ERROR_LIST_EMPTY_MESSAGE, HttpStatus.BAD_REQUEST,
            "Danh sách dữ liệu đang trống, không thể xử lý yêu cầu"),

    ERROR_EMAIL_REQUIRED(BAD_REQUEST, EMAIL_REQUIRED_MESSAGE, HttpStatus.BAD_REQUEST,
            "Trường email là bắt buộc, vui lòng nhập email"),

    ERROR_TOKEN_INVALID(BAD_REQUEST, ERROR_TOKEN_INVALID_MESSAGE, HttpStatus.BAD_REQUEST,
            "Token không hợp lệ hoặc đã hết hạn, vui lòng đăng nhập lại"),

    USERNAME_EXISTS(BAD_REQUEST, USERNAME_EXIST, HttpStatus.BAD_REQUEST,
            "Tên đăng nhập đã tồn tại trong hệ thống"),

    EMAIL_EXISTS(BAD_REQUEST, EMAIL_EXIST, HttpStatus.BAD_REQUEST,
            "Email đã được sử dụng, vui lòng nhập email khác"),

    BALANCE_NOT_ENOUGH(BAD_REQUEST, BALANCE, HttpStatus.BAD_REQUEST,
            "Số dư tài khoản không đủ để thực hiện giao dịch"),

    DEFAULT_ROLE_NOT_FOUND(NOT_FOUND, DEFAULT_ROLE_NOT_VALID, HttpStatus.NOT_FOUND,
            "Không tìm thấy vai trò mặc định cho người dùng"),

    USER_NOT_FOUND(NOT_FOUND, USER_NOT_VALID, HttpStatus.NOT_FOUND,
            "Không tìm thấy thông tin người dùng trong hệ thống"),

    PAYMENT_NOT_FOUND(NOT_FOUND, PAYMENT_NOT_VALID, HttpStatus.BAD_REQUEST,
            "Phương thức thanh toán không hợp lệ hoặc không tồn tại"),

    VIP_TYPE_NOT_FOUND(NOT_FOUND, VIP_TYPE_ERROR, HttpStatus.NOT_FOUND,
            "Không tìm thấy loại VIP phù hợp trong hệ thống"),

    VIP_PACKAGE_NOT_FOUND(NOT_FOUND, VIP_PACKAGE_ERROR, HttpStatus.NOT_FOUND,
            "Không tìm thấy gói VIP tương ứng"),

    NOT_ENOUGH_QUANTITY(NOT_FOUND, NOT_ENOUGH_QUANTITY_SYSTEM, HttpStatus.NOT_FOUND,
            "Số lượng sản phẩm trong kho không đủ để đặt hàng"),

    INVALID_PASSWORD(BAD_REQUEST, INVALID_PASSWORD_FAIL, HttpStatus.BAD_REQUEST,
            "Mật khẩu không chính xác, vui lòng thử lại"),

    INVALID_OTP(BAD_REQUEST, INVALID_OTP_FAIL, HttpStatus.BAD_REQUEST,
            "OTP không chính xác, vui lòng thử lại"),

    INVALID_PASSWORD_FORMAT(BAD_REQUEST, "Mật khẩu phải ít nhất 8 ký tự, bao gồm chữ, số và ký tự đặc biệt",
            HttpStatus.BAD_REQUEST, "Định dạng mật khẩu không hợp lệ"),

    INVALID_EMAIL_FORMAT(BAD_REQUEST, "Email không hợp lệ", HttpStatus.BAD_REQUEST,
            "Định dạng email không đúng, vui lòng kiểm tra lại");

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
    private final String description; // Thêm mô tả chi tiết

    ErrorCode(int code, String message, HttpStatus httpStatus, String description) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
        this.description = description;
    }
}
