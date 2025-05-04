package com.xin.graphdomainbackend.esdao;

import com.xin.graphdomainbackend.model.entity.es.EsUser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

@SpringBootTest
class EsUserDaoTest {

    @Resource
    private EsUserDao esUserDao;

    @Test
    void findByUserAccount() {

        // 1. 删除所有数据
        esUserDao.deleteAll();

        // 2. 新建并保存一条指定 id 的数据
        EsUser userTest = new EsUser();
        userTest.setId(123456L); // 自己赋值 id
        userTest.setUserAccount("402130563");
        userTest.setUserName("测试用户");
        esUserDao.save(userTest);

        System.out.println("ES中所有数据：");
        esUserDao.findAll().forEach(System.out::println);

        Optional<EsUser> userOptional = esUserDao.findByUserAccount("402130563");
        System.out.println("查询结果: " + userOptional);

        if (userOptional.isPresent()) {
            EsUser user = userOptional.get();
            for (Field field : user.getClass().getDeclaredFields()) {
                field.setAccessible(true); //允许访问私有字段
                try {
                    System.out.println(field.getName() + ":" + field.get(user));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("没查到");
        }

    }

    @Test
     void findByUserAccountContaining() {

        // 新建并保存一条指定 id 的数据
        EsUser userTest = new EsUser();
        userTest.setId(3L); // 自己赋值 id
        userTest.setUserAccount("402130564");
        userTest.setUserName("测试用户");
        esUserDao.save(userTest);

        // 模糊查询
        List<EsUser> users = esUserDao.findByUserAccountContaining("40213");
        System.out.println("模糊查询结果数量: " + users.size());

        for(EsUser esUser : users) {
            for (Field field : esUser.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    System.out.println(field.getName() + "：" + field.get(esUser));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }


    }

}