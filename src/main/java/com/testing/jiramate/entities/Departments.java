package com.testing.jiramate.entities;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class Departments {
    @Value(value = "${testingcenter}")
    private String[] department;
}
