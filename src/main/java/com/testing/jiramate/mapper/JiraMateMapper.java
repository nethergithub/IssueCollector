package com.testing.jiramate.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface JiraMateMapper {

    @Select({"SELECT jobid FROM projectmanager.t_employee WHERE deptype = (SELECT deptype FROM projectmanager.t_department WHERE depname = #{deptname})"})
    String[] getEmployeesByDeptName(String deptname);

}
