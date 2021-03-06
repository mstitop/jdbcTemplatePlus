/**
 * @(#)AutoIncTest.java, 10月 13, 2019.
 * <p>
 *
 */
package io.github.skycloud.fastdao.demo;

import com.google.common.collect.Lists;
import io.github.skycloud.fastdao.core.ast.conditions.Condition;
import io.github.skycloud.fastdao.core.ast.request.Request;
import io.github.skycloud.fastdao.core.ast.conditions.EqualCondition;
import io.github.skycloud.fastdao.core.ast.conditions.EqualConditionAst;
import io.github.skycloud.fastdao.core.ast.enums.SqlFunEnum;
import io.github.skycloud.fastdao.core.ast.model.SqlFunction;
import io.github.skycloud.fastdao.core.ast.request.CountRequestAst;
import io.github.skycloud.fastdao.core.ast.request.QueryRequest;
import io.github.skycloud.fastdao.core.ast.request.QueryRequestAst;
import io.github.skycloud.fastdao.core.ast.request.UpdateRequestAst;
import io.github.skycloud.fastdao.core.reflection.MetaClass;
import io.github.skycloud.fastdao.core.reflection.MetaField;
import io.github.skycloud.fastdao.core.models.QueryResult;
import io.github.skycloud.fastdao.demo.dao.AutoIncDAO;
import io.github.skycloud.fastdao.demo.model.Model;
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

import static io.github.skycloud.fastdao.demo.model.Columns.CREATED;
import static io.github.skycloud.fastdao.demo.model.Columns.DELETED;
import static io.github.skycloud.fastdao.demo.model.Columns.ID;
import static io.github.skycloud.fastdao.demo.model.Columns.NAME;
import static io.github.skycloud.fastdao.demo.model.Columns.UPDATED;

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
        Model model = dao.selectByPrimaryKey(1L);
        log.info(model.toString());
    }

    @Test
    public void test_select_by_primay_key_null() {
        Model model = dao.selectByPrimaryKey(6L);
        log.info(model.toString());
    }

    @Test
    public void test_select_by_primary_keys() {
        List<Model> models = dao.selectByPrimaryKeys(Lists.newArrayList(1L, 2L, 3L, 4L, 5L, 6L));
        log.info(models.toString());
    }

    @Test
    @Transactional
    public void test_update_by_primary_key() {
        Model model = getDefaultModel();
        model.setId(6L);
        model.setDeleted(true);
        int count = dao.updateByPrimaryKey(model);
        Assert.assertEquals(1, count);
        Model fromDB = dao.selectByPrimaryKey(6L);
        assertEqual(model, fromDB);
    }

    @Test
    @Transactional
    public void test_update_by_primary_key_not_exist() {
        Model model = getDefaultModel();
        model.setId(10L);
        int count = dao.updateByPrimaryKey(model);
        Assert.assertEquals(0, count);
        Assert.assertNull(dao.selectByPrimaryKey(10L));
    }

    @Test
    @Transactional
    public void test_update_by_primary_key_selective() {
        Model model = getDefaultModel();
        model.setId(5L);
        model.setText(null);
        int count = dao.updateByPrimaryKeySelective(model);
        Assert.assertEquals(1, count);
        Model fromDB = dao.selectByPrimaryKey(5L);
        assertEqual(model, dao.selectByPrimaryKey(5L), "text");
        Assert.assertEquals("text 5", fromDB.getText());
    }

    @Test(expected = BadSqlGrammarException.class)
    @Transactional
    public void test_update_by_primary_key_selective_all_null() {
        Model model = new Model();
        model.setId(5L);
        dao.updateByPrimaryKeySelective(model);
    }

    @Test
    @Transactional
    public void test_update_by_primary_key_selective_not_exist() {
        Model model = getDefaultModel();
        model.setId(10L);
        int count = dao.updateByPrimaryKeySelective(model);
        Assert.assertEquals(0, count);
        Assert.assertNull(dao.selectByPrimaryKey(10L));
    }

    @Test
    @Transactional
    public void test_insert() {
        Model model = getDefaultModel();
        dao.insert(model);
        Model fromDb = dao.selectByPrimaryKey(model.getId());
        assertEqual(model, fromDb);
    }

    @Test(expected = DuplicateKeyException.class)
    @Transactional
    public void test_insert_exist() {
        Model model = getDefaultModel();
        model.setId(5L);
        dao.insert(model);
        log.info(dao.selectByPrimaryKey(5L).toString());
    }

    @Test//(expected = DataIntegrityViolationException.class)
    @Transactional
    public void test_insert_all_null() {
        Model model = new Model();
        dao.insert(model);

    }

    @Test
    @Transactional
    public void test_insert_selective() {
        Model model = getDefaultModel();
        model.setName("insert_selective");
        dao.insertSelective(model);
        assertEqual(model, dao.selectByPrimaryKey(model.getId()));
    }

    @Test
    @Transactional
    public void test_insert_selective_text_name_null() {
        Model model = getDefaultModel();
        model.setText(null);
        model.setName(null);
        dao.insertSelective(model);
        assertEqual(model, dao.selectByPrimaryKey(model.getId()), "name");
        Assert.assertEquals("", dao.selectByPrimaryKey(model.getId()).getName());
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
        CountRequestAst request = new CountRequestAst();
        request.setCondition(DELETED.eq(true));
        int count = dao.count(request);
        Assert.assertEquals(3, count);
        request.setCondition(DELETED.eq(false));
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
        QueryRequestAst request = new QueryRequestAst();
        request.setCondition(ID.eq(Lists.newArrayList(1, 2, 3)));
        List<Model> models = dao.select(request);
        request.setCondition(ID.eq(1, 2, 3));
        List<Model> models2 = dao.select(request);
        for (int i = 0; i < models.size(); i++) {
            assertEqual(models.get(i), models2.get(i));
        }
    }

    @Test
    public void test_select_multi_condition() {
        QueryRequestAst request = new QueryRequestAst();
        request.setCondition(Condition.and()
                .and(ID.eq(1, 2, 3, 4, 5))
                .and(DELETED.gt(0))
                .and(UPDATED.lt(new Date()))
                .and(NAME.like("i").matchLeft().matchRight()));

        List<Model> models = dao.select(request);
        Assert.assertEquals(2, models.size());
        System.out.println(models);
        Assert.assertTrue(models.stream().allMatch(x -> Lists.newArrayList(1L, 3L).contains(x.getId())));
    }

    @Test
    public void test_select_empty_condition() {
        QueryRequestAst request = new QueryRequestAst();
        request.setCondition(NAME.eq(Lists.newArrayList()))
                .onSyntaxError(e -> Lists.newArrayList());

        Assert.assertTrue(CollectionUtils.isEmpty(dao.select(request)));
    }

    @Test
    public void test_select_ignore_illegal_condition() {
        QueryRequestAst request = new QueryRequestAst();
        request.setCondition(Condition.and().andOptional(ID.eq(Lists.newArrayList())).allowEmpty());
        List<Model> models = dao.select(request);
        Assert.assertEquals(6, models.size());
    }

    @Test
    public void test_select_complex_condition() {
        QueryRequestAst request = new QueryRequestAst();
        Condition condition = Condition.and()
                .andOptional(ID.eq(Lists.newArrayList()))
                .andOptional(Condition.or())
                .andOptional(Condition.and().and(ID.eq(1, 2, 3, 4, 5, 6)))
                .andOptional(Condition.or()
                        .or(ID.gt(0))
                        .orOptional(Condition.and()));
        request.setCondition(condition);
        List<Model> models = dao.select(request);
        Assert.assertEquals(6, models.size());
    }

    //************ count(QueryRequest request) test **************

    @Test
    @Transactional
    public void test_count_equal_multi() {
        CountRequestAst request = new CountRequestAst();
        request.setCondition(ID.eq(Lists.newArrayList(1, 2, 3)));
        int count = dao.count(request);
        Assert.assertEquals(3, count);
        request.setCondition(ID.eq(1, 2, 3));
        int count2 = dao.count(request);
        Assert.assertEquals(3, count2);
    }

    @Test
    public void test_count_multi_condition() {
        CountRequestAst request = new CountRequestAst();
        request.setCondition(Condition.and()
                .and(ID.eq(1, 2, 3, 4, 5))
                .and(DELETED.gt(0))
                .and(UPDATED.lt(new Date()))
                .and(NAME.like("i").matchLeft().matchRight()));

        int count = dao.count(request);
        Assert.assertEquals(2, count);

    }

    @Test
    public void test_count_empty_condition() {
        CountRequestAst request = new CountRequestAst();
        request.setCondition(NAME.eq(Lists.newArrayList()))
                .onSyntaxError(e -> 0);
        Assert.assertEquals(0, dao.count(request));
    }

    @Test
    public void test_count_ignore_illegal_condition() {
        CountRequestAst request = new CountRequestAst();
        request.setCondition(Condition.and().andOptional(ID.eq(Lists.newArrayList())).allowEmpty());
        int count = dao.count(request);
        Assert.assertEquals(6, count);
    }


    //************ update(UpdateRequest request) test **************

    @Test
    @Transactional
    public void test_update_equal_multi() {
        UpdateRequestAst request = new UpdateRequestAst();
        request.addUpdateField(NAME, "updated");
        request.setCondition(ID.eq(Lists.newArrayList(1, 2, 3)));
        int update = dao.update(request);
        Assert.assertTrue(dao.selectByPrimaryKeys(1L, 2L, 3L).stream()
                .map(Model::getName)
                .allMatch(x -> x.equals("updated")));
        Assert.assertEquals(3, update);
        request.setCondition(ID.eq(1, 2, 3));
        int update2 = dao.update(request);
        Assert.assertEquals(3, update2);
    }

    @Test
    @Transactional
    public void test_update_multi_condition() {
        UpdateRequestAst request = new UpdateRequestAst();
        request.setCondition(Condition.and()
                .and(ID.eq(1, 2, 3, 4, 5))
                .and(DELETED.gt(0))
                .and(UPDATED.lt(new Date()))
                .and(NAME.like("i").matchLeft().matchRight()));
        Date date = new Date();
        request.addUpdateField(CREATED, date);
        int update = dao.update(request);
        Assert.assertEquals(2, update);
        Assert.assertTrue(dao.selectByPrimaryKeys(1L, 3L).stream().map(Model::getCreated).allMatch(x -> x.equals(date)));
    }

    @Test
    @Transactional
    public void test_update_empty_condition() {
        UpdateRequestAst request = new UpdateRequestAst();
        request.setCondition(NAME.eq(Lists.newArrayList()))
                .onSyntaxError(e -> 0);
        request.addUpdateField(NAME, "updated");
        Assert.assertEquals(0, dao.update(request));
        Assert.assertTrue(dao.select(new QueryRequestAst()).stream().map(Model::getName)
                .noneMatch(x -> Objects.equals(x, "updated")));
    }

    @Test
    @Transactional
    public void test_update_ignore_illegal_condition() {
        UpdateRequestAst request = new UpdateRequestAst();
        request.setCondition(Condition.and().andOptional(ID.eq(Lists.newArrayList())).allowEmpty());
        request.addUpdateField(NAME, "updated");
        int update = dao.update(request);
        Assert.assertEquals(6, update);
        Assert.assertTrue(dao.select(new QueryRequestAst()).stream()
                .map(Model::getName).allMatch(x -> x.equals("updated")));
    }

    @Test
    @Transactional
    public void test_update_complex_condition() {
        UpdateRequestAst request = new UpdateRequestAst();
        Condition condition = Condition.and()
                .andOptional(ID.eq(Lists.newArrayList()))
                .andOptional(Condition.or())
                .andOptional(Condition.and().and(ID.eq(1, 2, 3, 4, 5, 6)))
                .andOptional(Condition.or()
                        .or(ID.gt(0))
                        .orOptional(Condition.and()));
        request.setCondition(condition);
        request.addUpdateField(NAME, "updated");
        int count = dao.update(request);
        Assert.assertEquals(6, count);
        Assert.assertTrue(dao.select(new QueryRequestAst()).stream()
                .map(Model::getName).allMatch(x -> x.equals("updated")));
    }


    private Model getDefaultModel() {
        Model model = new Model();
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

    @Test
    public void bindConditionTest() {
        QueryRequest request = Request.queryRequest()
                .addSelectFields(ID)
                .limit(10)
                .beginAndCondition()
                .and(ID.eq(3))
                .and(Condition
                        .or()
                        .or(ID.eq(3))
                        .or(ID.eq(4)))
                .endCondition();
        dao.select(request);

    }

    @Test
    public void testIsNullCondition() {
        QueryRequest request = Request.queryRequest()
                .beginAndCondition()
                .and(NAME.isNull())
                .and(NAME.eq(3))
                .allowEmpty()
                .endCondition();
        System.out.println(dao.select(request));
    }

    @Test
    public void testGet() {
        QueryRequest request = Request.queryRequest()
                .beginAndCondition()
                .and(Condition.and().allowEmpty())
                .allowEmpty()
                .endCondition()
                .onSyntaxError(e -> Lists.newArrayList());


    }
    @Test
    public void test_function(){
        QueryRequest request=Request.queryRequest();
        request.addSelectFields(new SqlFunction(SqlFunEnum.MAX,ID));
        List<QueryResult<Model>> results=dao.selectAdvance(request);
    }
    @Test
    public  void test(){
        EqualCondition equalCondition=new EqualConditionAst("hello",Lists.newArrayList());
    }
}