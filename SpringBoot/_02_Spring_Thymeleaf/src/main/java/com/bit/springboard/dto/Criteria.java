package com.bit.springboard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Criteria {
    private int pageNum;
    private int amount;
    private int startNum;

    // 기본생성자로 밑에있는 생성자 호출
    public Criteria() {
        this(1, 10);
    }

    public Criteria(int pageNum, int amount) {
        this.pageNum = pageNum;
        this.amount = amount;
    }
}
