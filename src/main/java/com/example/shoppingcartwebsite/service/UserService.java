package com.example.shoppingcartwebsite.service;

import com.example.shoppingcartwebsite.model.UserDtls;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {

    UserDtls saveUser(UserDtls user);

    UserDtls getUserByEmail(String email);

    List<UserDtls> getUsers(String role);

    Boolean updateAccountStatus(Integer id, Boolean status);

    void increaseFailedAttempt(UserDtls user);

    void userAccountLock(UserDtls user);

    boolean unlockAccountTimeExpired(UserDtls user);

    void resetAttempt(int userId);

    void updateUserResetToken(String email, String resetToken);

    UserDtls getUserByToken(String token);

    UserDtls updateUser(UserDtls user);

    UserDtls updateUserProfile(UserDtls user, MultipartFile img);

    UserDtls saveAdmin(UserDtls user);

    Boolean existsEmail(String email);
    
    // Additional methods for better organization
    UserDtls findByResetToken(String token); // This method can be added to the interface.
    
    List<UserDtls> findByRole(String role); // This method can be added to the interface.
}
