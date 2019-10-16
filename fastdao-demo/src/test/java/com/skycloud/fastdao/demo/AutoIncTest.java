/**
 * @(#)AutoIncTest.java, 10月 13, 2019.
 * <p>
 * Copyright 2019 fenbi.com. All rights reserved.
 * FENBI.COM PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.skycloud.fastdao.demo;

import com.google.common.collect.Lists;
import com.skycloud.fastdao.core.ast.Condition;
import com.skycloud.fastdao.core.ast.request.CountRequest;
import com.skycloud.fastdao.core.ast.request.QueryRequest;
import com.skycloud.fastdao.core.ast.request.UpdateRequest;
import com.skycloud.fastdao.core.reflection.MetaClass;
import com.skycloud.fastdao.core.reflection.MetaField;
import com.skycloud.fastdao.demo.dao.AutoIncDAO;
import com.skycloud.fastdao.demo.model.AutoIncModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static com.skycloud.fastdao.demo.model.Columns.CREATED;
import static com.skycloud.fastdao.demo.model.Columns.DELETED;
import static com.skycloud.fastdao.demo.model.Columns.ID;
import static com.skycloud.fastdao.demo.model.Columns.NAME;
import static com.skycloud.fastdao.demo.model.Columns.UPDATED;

/**
 * @author yuntian
 */
@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = FastdaoDemoApplication.class)
public class AutoIncTest {

    @Autowired
    AutoIncDAO dao;

    @Test
    public void test_select_by_primary_key() {
        AutoIncModel model = dao.selectByPrimaryKey(1L);
        log.info(model.toString());
    }

    @Test
    public void test_select_by_primay_key_null() {
        AutoIncModel model = dao.selectByPrimaryKey(6L);
        log.info(model.toString());
    }

    @Test
    public void test_select_by_primary_keys() {
        List<AutoIncModel> models = dao.selectByPrimaryKeys(Lists.newArrayList(1L, 2L, 3L, 4L, 5L, 6L));
        log.info(models.toString());
    }

    @Test
    @Transactional
    public void test_update_by_primary_key() {
        AutoIncModel model = getDefaultModel();
        model.setId(6L);
        model.setDeleted(true);
        int count = dao.updateByPrimaryKey(model);
        Assert.assertEquals(1, count);
        AutoIncModel fromDB = dao.selectByPrimaryKey(6L);
        assertEqual(model, fromDB);
    }

    @Test
    @Transactional
    public void test_update_by_primary_key_not_exist() {
        AutoIncModel model = getDefaultModel();
        model.setId(10L);
        int count = dao.updateByPrimaryKey(model);
        Assert.assertEquals(0, count);
        Assert.assertNull(dao.selectByPrimaryKey(10L));
    }

    @Test
    @Transactional
    public void test_update_by_primary_key_selective() {
        AutoIncModel model = getDefaultModel();
        model.setId(5L);
        model.setText(null);
        int count = dao.updateByPrimaryKeySelective(model);
        Assert.assertEquals(1, count);
        AutoIncModel fromDB = dao.selectByPrimaryKey(5L);
        assertEqual(model, dao.selectByPrimaryKey(5L), "text");
        Assert.assertEquals("text 5", fromDB.getText());
    }

    @Test(expected = BadSqlGrammarException.class)
    @Transactional
    public void test_update_by_primary_key_selective_all_null() {
        AutoIncModel model = new AutoIncModel();
        model.setId(5L);
        dao.updateByPrimaryKeySelective(model);
    }

    @Test
    @Transactional
    public void test_update_by_primary_key_selective_not_exist() {
        AutoIncModel model = getDefaultModel();
        model.setId(10L);
        int count = dao.updateByPrimaryKeySelective(model);
        Assert.assertEquals(0, count);
        Assert.assertNull(dao.selectByPrimaryKey(10L));
    }

    @Test
    @Transactional
    public void test_insert() {
        AutoIncModel model = getDefaultModel();
        dao.insert(model);
        AutoIncModel fromDb = dao.selectByPrimaryKey(model.getId());
        assertEqual(model, fromDb);
    }

    @Test(expected = DuplicateKeyException.class)
    @Transactional
    public void test_insert_exist() {
        AutoIncModel model = getDefaultModel();
        model.setId(5L);
        dao.insert(model);
        log.info(dao.selectByPrimaryKey(5L).toString());
    }

    @Test(expected = DataIntegrityViolationException.class)
    @Transactional
    public void test_insert_all_null() {
        AutoIncModel model = new AutoIncModel();
        dao.insert(model);

    }

    @Test
    @Transactional
    public void test_insert_selective() {
        AutoIncModel model = getDefaultModel();
        model.setName("insert_selective");
        dao.insertSelective(model);
        assertEqual(model, dao.selectByPrimaryKey(model.getId()));
    }

    @Test
    @Transactional
    public void test_insert_selective_text_name_null() {
        AutoIncModel model = getDefaultModel();
        model.setText(null);
        model.setName(null);
        dao.insertSelective(model);
        assertEqual(model, dao.selectByPrimaryKey(7L), "name");
        Assert.assertEquals("", dao.selectByPrimaryKey(7L).getName());
    }

    @Test
    @Transactional
    public void test_delete() {
        int count = dao.deleteByPrimaryKey(5L);
        Assert.assertEquals(1, count);
        Assert.assertNull(dao.selectByPrimaryKey(5L));
    }

    @Test
    @Transactional
    public void test_count() {
        CountRequest request = new CountRequest();
        request.setCondition(DELETED.equal(true));
        int count = dao.count(request);
        Assert.assertEquals(3, count);
        request.setCondition(DELETED.equal(false));
        count = dao.count(request);
        Assert.assertEquals(3, count);
        request.setCondition(null);
        count = dao.count(request);
        Assert.assertEquals(6, count);
    }

    //************ select(QueryRequest request) test **************
    @Test
    @Transactional
    public void test_select_equal_multi() {
        QueryRequest request = new QueryRequest();
        request.setCondition(ID.equal(Lists.newArrayList(1, 2, 3)));
        List<AutoIncModel> models = dao.select(request);
        request.setCondition(ID.equal(1, 2, 3));
        List<AutoIncModel> models2 = dao.select(request);
        for (int i = 0; i < models.size(); i++) {
            assertEqual(models.get(i), models2.get(i));
        }
    }

    @Test
    public void test_select_multi_condition() {
        QueryRequest request = new QueryRequest();
        request.setCondition(Condition.andCondition()
                .and(ID.equal(1, 2, 3, 4, 5))
                .and(DELETED.gt(0))
                .and(UPDATED.lt(new Date()))
                .and(NAME.like("i").matchLeft().matchRight()));

        List<AutoIncModel> models = dao.select(request);
        Assert.assertEquals(2, models.size());
        System.out.println(models);
        Assert.assertTrue(models.stream().allMatch(x -> Lists.newArrayList(1L, 3L).contains(x.getId())));
    }

    @Test
    public void test_select_empty_condition() {
        QueryRequest request = new QueryRequest();
        request.setCondition(NAME.equal(Lists.newArrayList()));
        Assert.assertTrue(CollectionUtils.isEmpty(dao.select(request)));
    }

    @Test
    public void test_select_ignore_illegal_condition() {
        QueryRequest request = new QueryRequest();
        request.setCondition(Condition.andCondition().andIgnoreIllegal(ID.equal(Lists.newArrayList())));
        List<AutoIncModel> models = dao.select(request);
        Assert.assertEquals(6, models.size());
    }

    @Test
    public void test_select_complex_condition() {
        QueryRequest request = new QueryRequest();
        Condition condition = Condition.andCondition()
                .andIgnoreIllegal(ID.equal(Lists.newArrayList()))
                .andIgnoreIllegal(Condition.orCondition())
                .andIgnoreIllegal(Condition.andCondition().and(ID.equal(1, 2, 3, 4, 5, 6)))
                .andIgnoreIllegal(Condition.orCondition()
                        .or(ID.gt(0))
                        .orIgnoreIllegal(Condition.andCondition()));
        request.setCondition(condition);
        List<AutoIncModel> models = dao.select(request);
        Assert.assertEquals(6, models.size());
    }

    //************ count(QueryRequest request) test **************

    @Test
    @Transactional
    public void test_count_equal_multi() {
        CountRequest request = new CountRequest();
        request.setCondition(ID.equal(Lists.newArrayList(1, 2, 3)));
        int count = dao.count(request);
        Assert.assertEquals(3, count);
        request.setCondition(ID.equal(1, 2, 3));
        int count2 = dao.count(request);
        Assert.assertEquals(3, count2);
    }

    @Test
    public void test_count_multi_condition() {
        CountRequest request = new CountRequest();
        request.setCondition(Condition.andCondition()
                .and(ID.equal(1, 2, 3, 4, 5))
                .and(DELETED.gt(0))
                .and(UPDATED.lt(new Date()))
                .and(NAME.like("i").matchLeft().matchRight()));

        int count = dao.count(request);
        Assert.assertEquals(2, count);

    }

    @Test
    public void test_count_empty_condition() {
        CountRequest request = new CountRequest();
        request.setCondition(NAME.equal(Lists.newArrayList()));
        Assert.assertEquals(0, dao.count(request));
    }

    @Test
    public void test_count_ignore_illegal_condition() {
        CountRequest request = new CountRequest();
        request.setCondition(Condition.andCondition().andIgnoreIllegal(ID.equal(Lists.newArrayList())));
        int count = dao.count(request);
        Assert.assertEquals(6, count);
    }


    //************ update(UpdateRequest request) test **************

    @Test
    @Transactional
    public void test_update_equal_multi() {
        UpdateRequest request = new UpdateRequest();
        request.addUpdateField(NAME, "updated");
        request.setCondition(ID.equal(Lists.newArrayList(1, 2, 3)));
        int update = dao.update(request);
        Assert.assertTrue(dao.selectByPrimaryKeys(1L, 2L, 3L).stream()
                .map(AutoIncModel::getName)
                .allMatch(x -> x.equals("updated")));
        Assert.assertEquals(3, update);
        request.setCondition(ID.equal(1, 2, 3));
        int update2 = dao.update(request);
        Assert.assertEquals(3, update2);
    }

    @Test
    @Transactional
    public void test_update_multi_condition() {
        UpdateRequest request = new UpdateRequest();
        request.setCondition(Condition.andCondition()
                .and(ID.equal(1, 2, 3, 4, 5))
                .and(DELETED.gt(0))
                .and(UPDATED.lt(new Date()))
                .and(NAME.like("i").matchLeft().matchRight()));
        Date date = new Date();
        request.addUpdateField(CREATED, date);
        int update = dao.update(request);
        Assert.assertEquals(2, update);
        Assert.assertTrue(dao.selectByPrimaryKeys(1L, 3L).stream().map(AutoIncModel::getCreated).allMatch(x -> x.equals(date)));
    }

    @Test
    @Transactional
    public void test_update_empty_condition() {
        UpdateRequest request = new UpdateRequest();
        request.setCondition(NAME.equal(Lists.newArrayList()));
        request.addUpdateField(NAME, "updated");
        Assert.assertEquals(0, dao.update(request));
        Assert.assertTrue(dao.select(new QueryRequest()).stream().map(AutoIncModel::getName)
                .noneMatch(x -> Objects.equals(x, "updated")));
    }

    @Test
    @Transactional
    public void test_update_ignore_illegal_condition() {
        UpdateRequest request = new UpdateRequest();
        request.setCondition(Condition.andCondition().andIgnoreIllegal(ID.equal(Lists.newArrayList())));
        request.addUpdateField(NAME, "updated");
        int update = dao.update(request);
        Assert.assertEquals(6, update);
        Assert.assertTrue(dao.select(new QueryRequest()).stream()
                .map(AutoIncModel::getName).allMatch(x -> x.equals("updated")));
    }

    @Test
    @Transactional
    public void test_update_complex_condition() {
        UpdateRequest request = new UpdateRequest();
        Condition condition = Condition.andCondition()
                .andIgnoreIllegal(ID.equal(Lists.newArrayList()))
                .andIgnoreIllegal(Condition.orCondition())
                .andIgnoreIllegal(Condition.andCondition().and(ID.equal(1, 2, 3, 4, 5, 6)))
                .andIgnoreIllegal(Condition.orCondition()
                        .or(ID.gt(0))
                        .orIgnoreIllegal(Condition.andCondition()));
        request.setCondition(condition);
        request.addUpdateField(NAME, "updated");
        int count = dao.update(request);
        Assert.assertEquals(6, count);
        Assert.assertTrue(dao.select(new QueryRequest()).stream()
                .map(AutoIncModel::getName).allMatch(x -> x.equals("updated")));
    }


    private AutoIncModel getDefaultModel() {
        AutoIncModel model = new AutoIncModel();
        model.setName("sky");
        model.setText("cloud");
        model.setCreated(new Date());
        model.setUpdated(new Date());
        model.setLongTime(new Date().getTime());
        model.setDeleted(false);
        return model;
    }


    private <T> void assertEqual(T model, T model2, String... exclude) {
        MetaClass metaClass = MetaClass.of(model.getClass());
        for (MetaField field : metaClass.metaFields()) {
            if (Arrays.asList(exclude).contains(field.getFieldName())) {
                continue;
            }
            Assert.assertEquals(field.invokeGetter(model), field.invokeGetter(model2));
        }
    }

}