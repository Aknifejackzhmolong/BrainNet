package com.brainsci.security.service;

import com.brainsci.security.entity.UserEntity;
import com.brainsci.security.exception.LoginException;
import com.brainsci.security.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    //根据用户名返回一个User实例
    public UserEntity findByUsername(String username){
        List<UserEntity> list = userRepository.findByUsername(username);
        return list.isEmpty() ? null : list.get(0);
    }

    //根据用户名返回一个User实例
    public UserEntity findByEmail(String email){
        List<UserEntity> list = userRepository.findByEMail(email);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 得到密文
     *
     * @param plaintext 明文
     */
    public String getCipherText(String plaintext) {
        return userRepository.getBcryt(plaintext);
    }

    /**
     * 检查密码是否正确
     *
     * @param userEntity 用户实体
     * @param plaintext  明文
     */
    public boolean checkPlainText(UserEntity userEntity, String plaintext) {
        String cipherText = getCipherText(plaintext);
        return cipherText.equals(userEntity.getPassword());
    }

    /**
     * 改变用户密码
     *
     * @param userEntity   用户实体
     * @param oldPlaintext 原明文
     * @param newPlaintext 新明文
     */
    public boolean changePassword(UserEntity userEntity, String oldPlaintext, String newPlaintext) {
        if (checkPlainText(userEntity, oldPlaintext)) {
            userEntity.setPassword(getCipherText(newPlaintext));
            userRepository.save(userEntity);
            return true;
        }
        return false;
    }

    public void createAccount(UserEntity userEntity){
        userRepository.saveAndFlush(userEntity);
    }
}
