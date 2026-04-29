package com.sismics.docs.core.util.action;

import com.sismics.docs.BaseTransactionalTest;
import com.sismics.docs.core.dao.DocumentDao;
import com.sismics.docs.core.dao.TagDao;
import com.sismics.docs.core.dao.criteria.TagCriteria;
import com.sismics.docs.core.dao.dto.DocumentDto;
import com.sismics.docs.core.dao.dto.TagDto;
import com.sismics.docs.core.model.jpa.Document;
import com.sismics.docs.core.model.jpa.Tag;
import com.sismics.docs.core.model.jpa.User;
import org.junit.Assert;
import org.junit.Test;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.util.Date;
import java.util.List;

public class TestAction extends BaseTransactionalTest {

    @Test
    public void testProcessFilesActionValidate() {
        ProcessFilesAction action = new ProcessFilesAction();
        // validate() is a no-op, should not throw
        action.validate(null);
        action.validate(Json.createObjectBuilder().build());
    }

    @Test
    public void testAddTagActionValidate() throws Exception {
        User user = createUser("testActionUser");

        // Create a tag
        TagDao tagDao = new TagDao();
        Tag tag = new Tag();
        tag.setName("TestTag");
        tag.setColor("#ff0000");
        tag.setUserId(user.getId());
        String tagId = tagDao.create(tag, user.getId());

        // Validate with valid tag should succeed
        AddTagAction action = new AddTagAction();
        JsonObject validAction = Json.createObjectBuilder().add("tag", tagId).build();
        action.validate(validAction);

        // Validate with invalid tag ID should throw
        JsonObject invalidTag = Json.createObjectBuilder().add("tag", "invalid-id").build();
        try {
            action.validate(invalidTag);
            Assert.fail("Should have thrown exception for invalid tag");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("not a valid tag"));
        }
    }

    @Test
    public void testRemoveTagActionValidate() throws Exception {
        User user = createUser("testActionUser2");

        TagDao tagDao = new TagDao();
        Tag tag = new Tag();
        tag.setName("RemoveTag");
        tag.setColor("#00ff00");
        tag.setUserId(user.getId());
        String tagId = tagDao.create(tag, user.getId());

        RemoveTagAction action = new RemoveTagAction();
        JsonObject validAction = Json.createObjectBuilder().add("tag", tagId).build();
        action.validate(validAction);
    }

    @Test
    public void testAddTagActionExecute() throws Exception {
        User user = createUser("testActionUser3");

        // Create a tag
        TagDao tagDao = new TagDao();
        Tag tag = new Tag();
        tag.setName("ExecuteTag");
        tag.setColor("#0000ff");
        tag.setUserId(user.getId());
        String tagId = tagDao.create(tag, user.getId());

        // Create a document
        DocumentDao documentDao = new DocumentDao();
        Document document = new Document();
        document.setUserId(user.getId());
        document.setLanguage("eng");
        document.setTitle("Test Document");
        document.setCreateDate(new Date());
        String documentId = documentDao.create(document, user.getId());

        // Execute add tag action
        AddTagAction action = new AddTagAction();
        DocumentDto documentDto = new DocumentDto();
        documentDto.setId(documentId);
        JsonObject actionData = Json.createObjectBuilder().add("tag", tagId).build();
        action.execute(documentDto, actionData);

        // Verify tag was added
        List<TagDto> tagDtoList = tagDao.findByCriteria(new TagCriteria().setDocumentId(documentId), null);
        Assert.assertEquals(1, tagDtoList.size());
        Assert.assertEquals(tagId, tagDtoList.get(0).getId());
    }

    @Test
    public void testRemoveTagActionExecute() throws Exception {
        User user = createUser("testActionUser4");

        // Create a tag
        TagDao tagDao = new TagDao();
        Tag tag = new Tag();
        tag.setName("RemoveExecuteTag");
        tag.setColor("#ff00ff");
        tag.setUserId(user.getId());
        String tagId = tagDao.create(tag, user.getId());

        // Create a document
        DocumentDao documentDao = new DocumentDao();
        Document document = new Document();
        document.setUserId(user.getId());
        document.setLanguage("eng");
        document.setTitle("Test Document");
        document.setCreateDate(new Date());
        String documentId = documentDao.create(document, user.getId());

        // First add the tag
        tagDao.updateTagList(documentId, java.util.Collections.singleton(tagId));

        // Verify tag is there
        List<TagDto> tagDtoList = tagDao.findByCriteria(new TagCriteria().setDocumentId(documentId), null);
        Assert.assertEquals(1, tagDtoList.size());

        // Execute remove tag action
        RemoveTagAction action = new RemoveTagAction();
        DocumentDto documentDto = new DocumentDto();
        documentDto.setId(documentId);
        JsonObject actionData = Json.createObjectBuilder().add("tag", tagId).build();
        action.execute(documentDto, actionData);

        // Verify tag was removed
        tagDtoList = tagDao.findByCriteria(new TagCriteria().setDocumentId(documentId), null);
        Assert.assertEquals(0, tagDtoList.size());
    }
}
