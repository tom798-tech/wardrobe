package com.tom.wardrobe.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserVo extends User {

    private String newpsw;
}