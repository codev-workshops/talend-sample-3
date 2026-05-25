package com.migration.etl.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    private Integer id;
    private String first_name;
    private String last_name;
    private String city;
    private String state_cd;
    private Integer flavor;
}
