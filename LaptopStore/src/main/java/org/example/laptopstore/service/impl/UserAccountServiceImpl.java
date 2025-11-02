package org.example.laptopstore.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.laptopstore.dto.request.account.ChangePasswordRequest;
import org.example.laptopstore.dto.request.account.LoginRequest;
import org.example.laptopstore.dto.request.account.RegisterRequest;
import org.example.laptopstore.dto.request.account.ResetPasswordRequest;
import org.example.laptopstore.dto.response.account.LoginResponse;
import org.example.laptopstore.dto.response.account.RegisterReponse;
import org.example.laptopstore.dto.response.user.UserAdminResponse;
import org.example.laptopstore.entity.Role;
import org.example.laptopstore.entity.User;
import org.example.laptopstore.exception.AppException;
import org.example.laptopstore.exception.BadRequestException;
import org.example.laptopstore.exception.ErrorCode;
import org.example.laptopstore.exception.NotFoundException;
import org.example.laptopstore.repository.RoleRepository;
import org.example.laptopstore.repository.UserRepository;
import org.example.laptopstore.service.AuthenticationService;
import org.example.laptopstore.service.UserAccountService;
import org.example.laptopstore.util.Validation;
import org.example.laptopstore.util.enums.Status;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.example.laptopstore.exception.ErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl implements UserAccountService {
    private final EmailService emailService;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final AuthenticationService authenticationService;


    // Lưu OTP tạm thời trong bộ nhớ
    private final Map<String, String> otpCache = new ConcurrentHashMap<>();

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email) != null;
    }

    @Override
    public String generateOtp(String email) {
        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);
        otpCache.put(email, otp);
        return otp;
    }

    @Override
    @Transactional
    public RegisterReponse register(RegisterRequest request) {
        // Validate username
        if (userRepository.findByUsername(request.getUsername()) != null) {
            throw new AppException(ErrorCode.USERNAME_EXISTS);
        }

        //  Validate email format
        if (!Validation.isValidEmail(request.getEmail())) {
            throw new AppException(ErrorCode.INVALID_EMAIL_FORMAT);
        }

        //  Validate email uniqueness
        if (userRepository.findByEmail(request.getEmail()) != null) {
            throw new AppException(ErrorCode.EMAIL_EXISTS);
        }

        //  Validate password format
        if (!Validation.isValidPassword(request.getPassword())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD_FORMAT);
        }

        //  Nếu hợp lệ thì tạo user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(Status.ACTIVE);

        Role role = roleRepository.findByName("USER")
                .orElseThrow(() -> new AppException(ErrorCode.DEFAULT_ROLE_NOT_FOUND));
        user.setRole(role);

        return modelMapper.map(userRepository.save(user), RegisterReponse.class);
    }

    // Thêm getter cho OTP nếu cần xác minh ở controller
    public String getOtpByEmail(String email) {
        return otpCache.get(email);
    }


    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public User getUserById(Long idUser) {
        return userRepository.findById(idUser).orElseThrow();
    }

    public LoginResponse login(LoginRequest request) throws ParseException {
        User user = userRepository.findByUsername(request.getUsername());
        if (user == null) throw new AppException(USER_NOT_FOUND);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = generateToken(user);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole().getName())
                .email(user.getEmail())
                .build();
    }

    private String generateToken(User user) throws ParseException {
        return authenticationService.authenticate(user).getAccessToken();
    }

    @Override
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username);
        if (user == null) throw new AppException(USER_NOT_FOUND);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword()) ||
                !Validation.isValidPassword(request.getNewPassword())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public LoginResponse getUserById(String username) {
        User user = userRepository.findByUsername(username);
        return LoginResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .build();
    }

    @Override
    public Page<UserAdminResponse> getUserByAdmin(String keyword, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), sortBy));
        Page<User> users = userRepository.findAllByKeyword(keyword, pageable);
        return users.map(user -> modelMapper.map(user, UserAdminResponse.class));
    }

    @Override
    public void blockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND.getMessage()));
        user.setIsBlocked(!user.getIsBlocked());
        userRepository.save(user);
    }

    @Override
    public BigDecimal getBalance(Long id) {
        return userRepository.findBalanceByUserId(id);
    }

    @Override
    public void updateBalance(Long id, BigDecimal amount) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(USER_NOT_FOUND));

        BigDecimal oldBalance = user.getBalance();
        if (oldBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        user.setBalance(oldBalance.subtract(amount));
        userRepository.save(user);
    }

    @Override
    public Integer totalCustomerByMonthAndYear(Integer month, Integer year) {
        return userRepository.getTotalUserByMonthAndYear(month, year);
    }

    @Override
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        // Tạo OTP 6 chữ số
        String otp = String.valueOf((int)(Math.random() * 900000) + 100000);

        // Gửi OTP qua email
        emailService.sendOtpForPasswordReset(email, otp);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD); // Mật khẩu không khớp
        }

        // Kiểm tra OTP
        boolean validOtp = emailService.verifyOtp(request.getEmail(), request.getOtp());
        if (!validOtp) {
            throw new AppException(ErrorCode.INVALID_OTP); // Hoặc tạo ErrorCode OTP_INVALID
        }

        User user = userRepository.findByEmail(request.getEmail());
        if (user == null) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

}
