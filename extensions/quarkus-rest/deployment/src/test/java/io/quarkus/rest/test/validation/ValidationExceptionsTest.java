package io.quarkus.rest.test.validation;

import static org.jboss.resteasy.utils.PortProviderUtil.generateURL;

import java.util.List;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.resteasy.api.validation.ResteasyConstraintViolation;
import org.jboss.resteasy.api.validation.ResteasyViolationException;
import org.jboss.resteasy.api.validation.Validation;
import org.jboss.resteasy.plugins.validation.ResteasyViolationExceptionImpl;
import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.rest.test.validation.resource.ValidationExceptionClassValidator;
import io.quarkus.rest.test.validation.resource.ValidationExceptionCrazyConstraint;
import io.quarkus.rest.test.validation.resource.ValidationExceptionCrazyValidator;
import io.quarkus.rest.test.validation.resource.ValidationExceptionIncorrectConstraint;
import io.quarkus.rest.test.validation.resource.ValidationExceptionMapper;
import io.quarkus.rest.test.validation.resource.ValidationExceptionOtherConstraint;
import io.quarkus.rest.test.validation.resource.ValidationExceptionOtherValidationException;
import io.quarkus.rest.test.validation.resource.ValidationExceptionOtherValidationException2;
import io.quarkus.rest.test.validation.resource.ValidationExceptionOtherValidationException3;
import io.quarkus.rest.test.validation.resource.ValidationExceptionOtherValidator;
import io.quarkus.rest.test.validation.resource.ValidationExceptionResourceCrazy;
import io.quarkus.rest.test.validation.resource.ValidationExceptionResourceWithIncorrectConstraint;
import io.quarkus.rest.test.validation.resource.ValidationExceptionResourceWithInvalidConstraintGroup;
import io.quarkus.rest.test.validation.resource.ValidationExceptionResourceWithOther;
import io.quarkus.rest.test.validation.resource.ValidationExceptionSubResourceWithInvalidOverride;
import io.quarkus.rest.test.validation.resource.ValidationExceptionSuperResource;
import io.quarkus.rest.test.validation.resource.ValidationExceptionTestGroup1;
import io.quarkus.rest.test.validation.resource.ValidationExceptionTestGroup2;

/**
 * @tpSubChapter Validator provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class ValidationExceptionsTest {

    protected static final Logger logger = LogManager.getLogger(ValidationExceptionsTest.class.getName());
    static QuarkusRestClient client;

    private static final String DEF_EXCEPTION = "constraintDefinitionException";
    private static final String CUSTOM_DEF_EXCEPTION = "customConstraintDefinitionException";
    private static final String DECL_EXCEPTION = "constraintDeclarationException";
    private static final String CUSTOM_DECL_EXCEPTION = "customConstraintDeclarationException";
    private static final String GROUP_DEF_EXCEPTION = "groupDefinitionException";
    private static final String CUSTOM_GROUP_DEF_EXCEPTION = "customGroupDefinitionException";
    private static final String OTHER_EXCEPTION = "otherException";
    private static final String CRAZY_EXCEPTION = "crazyException";
    private static final String ERROR_MESSAGE = "Expected other response";
    private static final String ERROR_HEADER_MESSAGE = "Header was null";
    private static final String ERROR_HEADER_VALIDATION_EXCEPTION_MESSAGE = "validation-expcetion header was expected to be true";

    public static Archive<?> deploy(String name, Class<?>... resourceClasses) throws Exception {
        WebArchive war = TestUtil.prepareArchive(name);
        war.addClass(ValidationExceptionClassValidator.class);
        war.addClass(ValidationExceptionCrazyConstraint.class);
        war.addClass(ValidationExceptionCrazyValidator.class);
        war.addClass(ValidationExceptionIncorrectConstraint.class);
        war.addClass(ValidationExceptionOtherConstraint.class);
        war.addClass(ValidationExceptionOtherValidationException.class);
        war.addClass(ValidationExceptionOtherValidationException2.class);
        war.addClass(ValidationExceptionOtherValidationException3.class);
        war.addClass(ValidationExceptionOtherValidator.class);
        war.addClass(ValidationExceptionTestGroup1.class);
        war.addClass(ValidationExceptionTestGroup2.class);
        war.addClass(ValidationExceptionMapper.class);
        return TestUtil.finishContainerPrepare(war, null, resourceClasses);
    }

    @Before
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    @Deployment(name = DEF_EXCEPTION)
    public static Archive<?> constraintDefinitionExceptionDeploy() throws Exception {
        return deploy(DEF_EXCEPTION, ValidationExceptionResourceWithIncorrectConstraint.class);
    }

    @Deployment(name = CUSTOM_DEF_EXCEPTION)
    public static Archive<?> customConstraintDefinitionExceptionDeploy() throws Exception {
        return deploy(CUSTOM_DEF_EXCEPTION, ValidationExceptionResourceWithIncorrectConstraint.class,
                ValidationExceptionResourceWithIncorrectConstraint.ConstraintDefinitionExceptionMapper.class);
    }

    @Deployment(name = DECL_EXCEPTION)
    public static Archive<?> constraintDeclarationExceptionDeploy() throws Exception {
        return deploy(DECL_EXCEPTION, ValidationExceptionSubResourceWithInvalidOverride.class,
                ValidationExceptionSuperResource.class);
    }

    @Deployment(name = CUSTOM_DECL_EXCEPTION)
    public static Archive<?> customConstraintDeclarationExceptionDeploy() throws Exception {
        return deploy(CUSTOM_DECL_EXCEPTION, ValidationExceptionSubResourceWithInvalidOverride.class,
                ValidationExceptionSuperResource.class,
                ValidationExceptionSubResourceWithInvalidOverride.ConstraintDeclarationExceptionMapper.class);
    }

    @Deployment(name = GROUP_DEF_EXCEPTION)
    public static Archive<?> groupDefinitionExceptionDeploy() throws Exception {
        return deploy(GROUP_DEF_EXCEPTION, ValidationExceptionResourceWithInvalidConstraintGroup.class);
    }

    @Deployment(name = CUSTOM_GROUP_DEF_EXCEPTION)
    public static Archive<?> customGroupDefinitionExceptionDeploy() throws Exception {
        return deploy(CUSTOM_GROUP_DEF_EXCEPTION, ValidationExceptionResourceWithInvalidConstraintGroup.class,
                ValidationExceptionResourceWithInvalidConstraintGroup.GroupDefinitionExceptionMapper.class);
    }

    @Deployment(name = OTHER_EXCEPTION)
    public static Archive<?> otherExceptionDeploy() throws Exception {
        return deploy(OTHER_EXCEPTION, ValidationExceptionResourceWithOther.class);
    }

    @Deployment(name = CRAZY_EXCEPTION)
    public static Archive<?> crazyExceptionDeploy() throws Exception {
        return deploy(CRAZY_EXCEPTION, ValidationExceptionResourceCrazy.class);
    }

    /**
     * @tpTestDetails Resource with incorrect constraint, constrain definitiont exception is expected
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @OperateOnDeployment(DEF_EXCEPTION)
    public void testConstraintDefinitionException() throws Exception {
        Response response = client.target(generateURL("/", DEF_EXCEPTION)).request().post(null);
        Assert.assertEquals(Status.INTERNAL_SERVER_ERROR, response.getStatus());
        String header = response.getStringHeaders().getFirst(Validation.VALIDATION_HEADER);
        Assert.assertNotNull(ERROR_HEADER_MESSAGE, header);
        Assert.assertTrue(ERROR_HEADER_VALIDATION_EXCEPTION_MESSAGE, Boolean.valueOf(header));
        String entity = response.readEntity(String.class);
        logger.info("entity: " + entity);
        Assert.assertTrue(ERROR_MESSAGE, entity.contains("ConstraintDefinitionException"));
    }

    @Test
    @OperateOnDeployment(CUSTOM_DEF_EXCEPTION)
    public void testCustomConstraintDefinitionException() throws Exception {
        Response response = client.target(generateURL("/", CUSTOM_DEF_EXCEPTION)).request().post(null);
        Assert.assertEquals(Status.INTERNAL_SERVER_ERROR, response.getStatus());
        String header = response.getStringHeaders().getFirst(Validation.VALIDATION_HEADER);
        Assert.assertNotNull(ERROR_HEADER_MESSAGE, header);
        Assert.assertTrue(ERROR_HEADER_VALIDATION_EXCEPTION_MESSAGE, Boolean.valueOf(header));
        String entity = response.readEntity(String.class);
        logger.info("entity: " + entity);
        Assert.assertTrue(ERROR_MESSAGE, entity.contains("ConstraintDefinitionException"));
        Assert.assertTrue(ERROR_MESSAGE,
                entity.contains(ValidationExceptionResourceWithIncorrectConstraint.ConstraintDefinitionExceptionMapper.class
                        .getName()));
    }

    /**
     * @tpTestDetails Resource with incorrect constraint declaration, constraint definition exception is expected
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @OperateOnDeployment(DECL_EXCEPTION)
    public void testConstraintDeclarationException() throws Exception {
        Response response = client.target(generateURL("/", DECL_EXCEPTION)).request().post(null);
        Assert.assertEquals(TestUtil.getErrorMessageForKnownIssue("JBEAP-3459"), Status.INTERNAL_SERVER_ERROR,
                response.getStatus());
        String header = response.getStringHeaders().getFirst(Validation.VALIDATION_HEADER);
        Assert.assertNotNull(ERROR_HEADER_MESSAGE, header);
        Assert.assertTrue(ERROR_HEADER_VALIDATION_EXCEPTION_MESSAGE, Boolean.valueOf(header));
        String entity = response.readEntity(String.class);
        logger.info("entity: " + entity);
        Assert.assertTrue(ERROR_MESSAGE, entity.contains("ConstraintDeclarationException"));
    }

    @Test
    @OperateOnDeployment(CUSTOM_DECL_EXCEPTION)
    public void testCustomConstraintDeclarationException() throws Exception {
        Response response = client.target(generateURL("/", CUSTOM_DECL_EXCEPTION)).request().post(null);
        Assert.assertEquals(TestUtil.getErrorMessageForKnownIssue("JBEAP-3459"),
                Status.INTERNAL_SERVER_ERROR, response.getStatus());
        String header = response.getStringHeaders().getFirst(Validation.VALIDATION_HEADER);
        Assert.assertNotNull(ERROR_HEADER_MESSAGE, header);
        Assert.assertTrue(ERROR_HEADER_VALIDATION_EXCEPTION_MESSAGE, Boolean.valueOf(header));
        String entity = response.readEntity(String.class);
        logger.info("entity: " + entity);
        Assert.assertTrue(ERROR_MESSAGE, entity.contains("ConstraintDeclarationException"));
        Assert.assertTrue(ERROR_MESSAGE,
                entity.contains(ValidationExceptionSubResourceWithInvalidOverride.ConstraintDeclarationExceptionMapper.class
                        .getName()));
    }

    /**
     * @tpTestDetails Resource with incorrect group definition, group definition exception is expected
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @OperateOnDeployment(GROUP_DEF_EXCEPTION)
    public void testGroupDefinitionException() throws Exception {
        Response response = client.target(generateURL("/", GROUP_DEF_EXCEPTION)).request().get();
        Assert.assertEquals(Status.INTERNAL_SERVER_ERROR, response.getStatus());
        String header = response.getStringHeaders().getFirst(Validation.VALIDATION_HEADER);
        Assert.assertNotNull(ERROR_HEADER_MESSAGE, header);
        Assert.assertTrue(ERROR_HEADER_VALIDATION_EXCEPTION_MESSAGE, Boolean.valueOf(header));
        String entity = response.readEntity(String.class);
        logger.info("entity: " + entity);
        Assert.assertTrue(ERROR_MESSAGE, entity.contains("GroupDefinitionException"));
    }

    @Test
    @OperateOnDeployment(CUSTOM_GROUP_DEF_EXCEPTION)
    public void testCustomGroupDefinitionException() throws Exception {
        Response response = client.target(generateURL("/", CUSTOM_GROUP_DEF_EXCEPTION)).request().get();
        Assert.assertEquals(Status.INTERNAL_SERVER_ERROR, response.getStatus());
        String header = response.getStringHeaders().getFirst(Validation.VALIDATION_HEADER);
        Assert.assertNotNull(ERROR_HEADER_MESSAGE, header);
        Assert.assertTrue(ERROR_HEADER_VALIDATION_EXCEPTION_MESSAGE, Boolean.valueOf(header));
        String entity = response.readEntity(String.class);
        logger.info("entity: " + entity);
        Assert.assertTrue(ERROR_MESSAGE, entity.contains("GroupDefinitionException"));
        Assert.assertTrue(ERROR_MESSAGE,
                entity.contains(
                        ValidationExceptionResourceWithInvalidConstraintGroup.GroupDefinitionExceptionMapper.class.getName()));
    }

    /**
     * @tpTestDetails Tests for: Exception thrown during validation of field, Exception thrown during validation of parameter,
     *                Exception thrown during validation of return value, Exception thrown by resource method
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @OperateOnDeployment(OTHER_EXCEPTION)
    public void testOtherValidationException() throws Exception {

        {
            Response response = client.target(generateURL("/parameter/fail", OTHER_EXCEPTION)).request()
                    .post(Entity.text("abc"));
            Assert.assertEquals(Status.INTERNAL_SERVER_ERROR, response.getStatus());
            String header = response.getStringHeaders().getFirst(Validation.VALIDATION_HEADER);
            Assert.assertNotNull(ERROR_HEADER_MESSAGE, header);
            Assert.assertTrue(ERROR_HEADER_VALIDATION_EXCEPTION_MESSAGE, Boolean.valueOf(header));
            String entity = response.readEntity(String.class);
            logger.info("entity: " + entity);
            Assert.assertTrue(ERROR_MESSAGE, entity.contains("ValidationException"));
            Assert.assertTrue(ERROR_MESSAGE, entity.contains("OtherValidationException"));
        }

        {
            Response response = client.target(generateURL("/parameter/ok", OTHER_EXCEPTION)).request().post(Entity.text("abc"));
            Assert.assertEquals(Status.INTERNAL_SERVER_ERROR, response.getStatus());
            String header = response.getStringHeaders().getFirst(Validation.VALIDATION_HEADER);
            Assert.assertNotNull(ERROR_HEADER_MESSAGE, header);
            Assert.assertTrue(ERROR_HEADER_VALIDATION_EXCEPTION_MESSAGE, Boolean.valueOf(header));
            String entity = response.readEntity(String.class);
            logger.info("entity: " + entity);
            Assert.assertTrue(ERROR_MESSAGE, entity.contains("ValidationException"));
            Assert.assertTrue(ERROR_MESSAGE, entity.contains("OtherValidationException"));
        }

        {
            Response response = client.target(generateURL("/return/ok", OTHER_EXCEPTION)).request().post(Entity.text("abc"));
            Assert.assertEquals(Status.INTERNAL_SERVER_ERROR, response.getStatus());
            String header = response.getStringHeaders().getFirst(Validation.VALIDATION_HEADER);
            Assert.assertNotNull(ERROR_HEADER_MESSAGE, header);
            Assert.assertTrue(ERROR_HEADER_VALIDATION_EXCEPTION_MESSAGE, Boolean.valueOf(header));
            String entity = response.readEntity(String.class);
            logger.info("entity: " + entity);
            Assert.assertTrue(ERROR_MESSAGE, entity.contains("ValidationException"));
            Assert.assertTrue(ERROR_MESSAGE, entity.contains("OtherValidationException"));
        }

        {
            Response response = client.target(generateURL("/execution/ok", OTHER_EXCEPTION)).request().get();
            Assert.assertEquals(Status.INTERNAL_SERVER_ERROR, response.getStatus());
            String header = response.getStringHeaders().getFirst(Validation.VALIDATION_HEADER);
            Assert.assertNotNull(ERROR_HEADER_MESSAGE, header);
            Assert.assertTrue(ERROR_HEADER_VALIDATION_EXCEPTION_MESSAGE, Boolean.valueOf(header));
            String entity = response.readEntity(String.class);
            logger.info("last entity: " + entity);
            Assert.assertTrue(ERROR_MESSAGE, entity.contains("OtherValidationException"));
            Assert.assertTrue(ERROR_MESSAGE, entity.contains("OtherValidationException2"));
            Assert.assertTrue(ERROR_MESSAGE, entity.contains("OtherValidationException3"));
        }

    }

    /**
     * @tpTestDetails Resource with crazy message in constraint
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @OperateOnDeployment(CRAZY_EXCEPTION)
    public void testCrazyMessage() throws Exception {
        Response response = client.target(generateURL("/", CRAZY_EXCEPTION)).request().get();
        Assert.assertEquals(Status.BAD_REQUEST, response.getStatus());
        String header = response.getStringHeaders().getFirst(Validation.VALIDATION_HEADER);
        Assert.assertNotNull(ERROR_HEADER_MESSAGE, header);
        Assert.assertTrue(ERROR_HEADER_VALIDATION_EXCEPTION_MESSAGE, Boolean.valueOf(header));
        ResteasyViolationException resteasyViolationException = new ResteasyViolationExceptionImpl(
                response.readEntity(String.class));
        List<ResteasyConstraintViolation> classViolations = resteasyViolationException.getClassViolations();
        Assert.assertEquals(1, classViolations.size());
        Assert.assertEquals(ValidationExceptionCrazyConstraint.DEFAULT_MESSAGE, classViolations.get(0).getMessage());
        logger.info("entity: " + resteasyViolationException);
    }
}
