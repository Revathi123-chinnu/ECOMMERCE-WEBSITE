package com.example.shoppingcartwebsite.service.Impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import com.example.shoppingcartwebsite.repository.UserRepository;
import com.example.shoppingcartwebsite.service.UserService;
import com.example.shoppingcartwebsite.util.AppConstant;
import com.example.shoppingcartwebsite.model.UserDtls;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDtls saveUser(UserDtls user) {
        user.setRole("ROLE_USER");
        user.setIsEnable(true);
        String encodePassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodePassword);
        return userRepository.save(user);
    }

    @Override
    public UserDtls getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public List<UserDtls> getUsers(String role) {
        return userRepository.findByRole(role);
    }

    @Override
    public Boolean updateAccountStatus(Integer id, Boolean status) {
        Optional<UserDtls> findByUser = userRepository.findById(id);
        if (findByUser.isPresent()) {
            UserDtls userDtls = findByUser.get();
            userDtls.setIsEnable(status);
            userRepository.save(userDtls);
            return true;
        }
        return false;
    }

    @Override
    public void increaseFailedAttempt(UserDtls user) {
        int attempt = user.getFailedAttempt() + 1;
        user.setFailedAttempt(attempt);
        userRepository.save(user);
    }

    @Override
    public void userAccountLock(UserDtls user) {
        user.setAccountNonLocked(false);
        user.setLockTime(new Date());
        userRepository.save(user);
    }

    @Override
    public boolean unlockAccountTimeExpired(UserDtls user) {
        if (user.getLockTime() == null) return false;

        long lockTime = user.getLockTime().getTime();
        long unLockTime = lockTime + AppConstant.UNLOCK_DURATION_TIME;
        long currentTime = System.currentTimeMillis();

        if (unLockTime < currentTime) {
            user.setAccountNonLocked(true);
            user.setFailedAttempt(0);
            user.setLockTime(null);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    @Override
    public void resetAttempt(int userId) {
        // Implementation needed
    }

    @Override
    public void updateUserResetToken(String email, String resetToken) {
        UserDtls findByEmail = userRepository.findByEmail(email);
        if (findByEmail != null) {
            findByEmail.setResetToken(resetToken);
            userRepository.save(findByEmail);
        }
    }

    @Override
    public UserDtls getUserByToken(String token) {
        return userRepository.findByResetToken(token);
    }

    @Override
    public UserDtls updateUser(UserDtls user) {
        return userRepository.save(user);
    }

    @Override
    public UserDtls updateUserProfile(UserDtls user, MultipartFile img) {
        UserDtls dbUser = userRepository.findById(user.getId()).orElse(null);

        if (dbUser != null) {
            if (img != null && !img.isEmpty()) {
                dbUser.setProfileImage(img.getOriginalFilename());
                saveProfileImage(img);
            }

            dbUser.setName(user.getName());
            dbUser.setMobileNumber(user.getMobileNumber());
            dbUser.setAddress(user.getAddress());
            dbUser.setCity(user.getCity());
            dbUser.setState(user.getState());
            dbUser.setPincode(user.getPincode());
            dbUser = userRepository.save(dbUser);
        }

        return dbUser;
    }

    private void saveProfileImage(MultipartFile img) {
        try {
            File saveFile = new File("src/main/resources/static/img/profile_img");
            if (!saveFile.exists()) {
                saveFile.mkdirs(); // Create directory if it does not exist
            }
            Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + img.getOriginalFilename());
            Files.copy(img.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public UserDtls saveAdmin(UserDtls user) {
        user.setRole("ROLE_ADMIN");
        user.setIsEnable(true);
        user.setAccountNonLocked(true);
        user.setFailedAttempt(0);

        String encodePassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodePassword);
        return userRepository.save(user);
    }

    @Override
    public Boolean existsEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public UserDtls findByResetToken(String token) {
        return userRepository.findByResetToken(token);
    }

    @Override
    public List<UserDtls> findByRole(String role) {
        return userRepository.findByRole(role);
    }
}
