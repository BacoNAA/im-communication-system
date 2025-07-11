package com.im.imcommunicationsystem.user.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Past;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/**
 * 更新用户资料请求DTO
 */
public class UpdateProfileRequest {

    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    @Size(max = 200, message = "个性签名长度不能超过200个字符")
    private String signature;

    private String avatarUrl;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phoneNumber;

    @Pattern(regexp = "^$|^(男|女|保密)$", message = "性别只能是：男、女、保密")
    private String gender;

    @Past(message = "生日必须是过去的日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    @Size(max = 100, message = "所在地长度不能超过100个字符")
    private String location;

    @Size(max = 100, message = "职业长度不能超过100个字符")
    private String occupation;

    // 构造函数
    public UpdateProfileRequest() {}

    public UpdateProfileRequest(String nickname, String signature, String avatarUrl) {
        this.nickname = nickname;
        this.signature = signature;
        this.avatarUrl = avatarUrl;
    }

    public UpdateProfileRequest(String nickname, String signature, String avatarUrl, 
                              String phoneNumber, String gender, LocalDate birthday, 
                              String location, String occupation) {
        this.nickname = nickname;
        this.signature = signature;
        this.avatarUrl = avatarUrl;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.birthday = birthday;
        this.location = location;
        this.occupation = occupation;
    }

    // Getter和Setter方法
    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    @Override
    public String toString() {
        return "UpdateProfileRequest{" +
                "nickname='" + nickname + '\'' +
                ", signature='" + signature + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", gender='" + gender + '\'' +
                ", birthday=" + birthday +
                ", location='" + location + '\'' +
                ", occupation='" + occupation + '\'' +
                '}';
    }
}