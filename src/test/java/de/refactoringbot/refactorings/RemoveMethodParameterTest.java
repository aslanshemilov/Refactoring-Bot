package de.refactoringbot.refactorings;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.javadoc.JavadocBlockTag;

import de.refactoringbot.model.botissue.BotIssue;
import de.refactoringbot.model.configuration.GitConfiguration;
import de.refactoringbot.model.exceptions.BotRefactoringException;
import de.refactoringbot.refactoring.RefactoringHelper;
import de.refactoringbot.refactoring.supportedrefactorings.RemoveMethodParameter;
import de.refactoringbot.resources.removeparameter.TestDataClassRemoveParameter;
import de.refactoringbot.resources.removeparameter.TestDataClassRemoveParameter.TestDataInnerClassRemoveParameter;
import de.refactoringbot.resources.removeparameter.TestDataClassWithCallOfTargetMethod;
import de.refactoringbot.resources.removeparameter.TestDataSiblingClassRemoveParameter;
import de.refactoringbot.resources.removeparameter.TestDataSubClassRemoveParameter;
import de.refactoringbot.resources.removeparameter.TestDataSuperClassRemoveParameter;

public class RemoveMethodParameterTest extends AbstractRefactoringTests {

	private static final Logger logger = LoggerFactory.getLogger(RemoveMethodParameterTest.class);

	private static final String SIBLING_CLASS_NAME = "TestDataSiblingClassRemoveParameter";
	private static final String SUB_CLASS_NAME = "TestDataSubClassRemoveParameter";
	private static final String SUPER_CLASS_NAME = "TestDataSuperClassRemoveParameter";
	private static final String CALL_OF_TARGET_METHOD_CLASS_NAME = "TestDataClassWithCallOfTargetMethod";
	private static final String TARGET_INNER_CLASS_NAME = "TestDataInnerClassRemoveParameter";
	private static final String TARGET_CLASS_NAME = "TestDataClassRemoveParameter";

	private TestDataClassRemoveParameter removeParameterTestClass = new TestDataClassRemoveParameter();
	private TestDataInnerClassRemoveParameter removeParameterInnerTestClass = removeParameterTestClass.new TestDataInnerClassRemoveParameter();
	private TestDataClassWithCallOfTargetMethod removeParameterCallerTestClass = new TestDataClassWithCallOfTargetMethod();
	private TestDataSuperClassRemoveParameter removeParameterSuperClass = new TestDataSuperClassRemoveParameter();
	private TestDataSubClassRemoveParameter removeParameterSubClass = new TestDataSubClassRemoveParameter();
	private TestDataSiblingClassRemoveParameter removeParameterSiblingClass = new TestDataSiblingClassRemoveParameter();

	private File fileOfTestClass;
	private File fileOfSuperClass;
	private File fileOfSubClass;
	private File fileWithCallerMethod;
	private File fileOfSiblingClass;

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Before
	public void createTempCopiesOfTestResourceFiles() throws IOException {
		fileOfTestClass = createTempCopyOfTestResourcesFile(TestDataClassRemoveParameter.class);
		fileOfSuperClass = createTempCopyOfTestResourcesFile(TestDataSuperClassRemoveParameter.class);
		fileOfSubClass = createTempCopyOfTestResourcesFile(TestDataSubClassRemoveParameter.class);
		fileWithCallerMethod = createTempCopyOfTestResourcesFile(TestDataClassWithCallOfTargetMethod.class);
		fileOfSiblingClass = createTempCopyOfTestResourcesFile(TestDataSiblingClassRemoveParameter.class);
	}

	@Test
	public void testTargetClassRefactored() throws Exception {
		// arrange
		List<File> filesToConsider = new ArrayList<File>();
		filesToConsider.add(fileOfTestClass);
		int lineNumberOfMethodWithParameterToBeRemoved = removeParameterTestClass.getLineOfMethodWithUnusedParameter(0,
				0, 0);
		String parameterName = "b";

		CompilationUnit cuOriginalFileOfTestClass = JavaParser.parse(fileOfTestClass);
		MethodDeclaration originalMethod = RefactoringHelper.getMethodDeclarationByLineNumber(
				lineNumberOfMethodWithParameterToBeRemoved, cuOriginalFileOfTestClass);
		MethodDeclaration originalDummyMethod = RefactoringHelper.getMethodDeclarationByLineNumber(
				removeParameterTestClass.getLineNumberOfDummyMethod(0, 0, 0), cuOriginalFileOfTestClass);
		MethodDeclaration originalCallerMethod = RefactoringHelper.getMethodDeclarationByLineNumber(
				removeParameterTestClass.getLineNumberOfCaller(), cuOriginalFileOfTestClass);
		MethodDeclaration originalCallerMethodInnerClass = RefactoringHelper.getMethodDeclarationByLineNumber(
				removeParameterInnerTestClass.getLineNumberOfCallerInInnerClass(), cuOriginalFileOfTestClass);
		MethodDeclaration originalMethodWithTargetMethodSignatureInInnerClass = RefactoringHelper
				.getMethodDeclarationByLineNumber(
						removeParameterInnerTestClass.getLineOfMethodWithUnusedParameter(0, 0, 0),
						cuOriginalFileOfTestClass);

		SoftAssertions softAssertions = new SoftAssertions();
		softAssertions.assertThat(originalMethod).isNotNull();
		softAssertions.assertThat(originalDummyMethod).isNotNull();
		softAssertions.assertThat(originalCallerMethod).isNotNull();
		softAssertions.assertThat(originalCallerMethodInnerClass).isNotNull();
		softAssertions.assertThat(originalMethodWithTargetMethodSignatureInInnerClass).isNotNull();
		softAssertions.assertAll();

		// act
		performRemoveParameter(filesToConsider, fileOfTestClass, lineNumberOfMethodWithParameterToBeRemoved,
				parameterName);

		// assert
		CompilationUnit cuRefactoredFileOfTestClass = JavaParser.parse(fileOfTestClass);
		MethodDeclaration refactoredMethod = getMethodByName(TARGET_CLASS_NAME, originalMethod.getNameAsString(),
				cuRefactoredFileOfTestClass);

		// assert that parameter has been removed from the target method
		assertThat(refactoredMethod).isNotNull();
		assertThat(refactoredMethod.getParameterByName(parameterName).isPresent()).isFalse();

		// assert that parameter has been removed from the Javadoc
		assertParameterNotPresentInJavadoc(refactoredMethod, parameterName);

		// assert that dummy method is unchanged
		MethodDeclaration dummyMethod = getMethodByName(TARGET_CLASS_NAME, originalDummyMethod.getNameAsString(),
				cuRefactoredFileOfTestClass);
		assertThat(dummyMethod).isNotNull();
		assertThat(dummyMethod.getParameterByName(parameterName)).isPresent();

		// assert that inner class method with same name as target method is unchanged
		MethodDeclaration methodWithTargetMethodSignatureInInnerClass = getMethodByName(TARGET_INNER_CLASS_NAME,
				originalMethodWithTargetMethodSignatureInInnerClass.getNameAsString(), cuRefactoredFileOfTestClass);
		assertThat(methodWithTargetMethodSignatureInInnerClass).isNotNull();
		assertThat(methodWithTargetMethodSignatureInInnerClass.getParameterByName(parameterName)).isPresent();

		// assert that caller method in same file has been refactored
		MethodDeclaration methodWithTargetMethodCalls = getMethodByName(TARGET_CLASS_NAME,
				originalCallerMethod.getNameAsString(), cuRefactoredFileOfTestClass);
		assertThat(methodWithTargetMethodCalls).isNotNull();
		assertAllMethodCallsArgumentSizeEqualToRefactoredMethodParameterCount(methodWithTargetMethodCalls,
				refactoredMethod);

		// assert that caller method in same file in inner class has been refactored
		MethodDeclaration methodWithTargetMethodCallInInnerClass = getMethodByName(TARGET_INNER_CLASS_NAME,
				originalCallerMethodInnerClass.getNameAsString(), cuRefactoredFileOfTestClass);
		assertThat(methodWithTargetMethodCallInInnerClass).isNotNull();
		assertAllMethodCallsArgumentSizeEqualToRefactoredMethodParameterCount(methodWithTargetMethodCallInInnerClass,
				refactoredMethod);
	}

	@Test
	public void testCallerClassRefactored() throws Exception {
		// arrange
		List<File> filesToConsider = new ArrayList<File>();
		filesToConsider.add(fileOfTestClass);
		filesToConsider.add(fileWithCallerMethod);
		int lineNumberOfMethodWithParameterToBeRemoved = removeParameterTestClass.getLineOfMethodWithUnusedParameter(0,
				0, 0);
		String parameterName = "b";

		CompilationUnit cuOriginalFileOfTestClass = JavaParser.parse(fileOfTestClass);
		CompilationUnit cuOriginalFileWithCallerMethod = JavaParser.parse(fileWithCallerMethod);
		MethodDeclaration originalMethod = RefactoringHelper.getMethodDeclarationByLineNumber(
				lineNumberOfMethodWithParameterToBeRemoved, cuOriginalFileOfTestClass);
		MethodDeclaration originalCallerMethodInDifferentFile = RefactoringHelper.getMethodDeclarationByLineNumber(
				removeParameterCallerTestClass.getLineOfCallerMethodInDifferentFile(), cuOriginalFileWithCallerMethod);

		SoftAssertions softAssertions = new SoftAssertions();
		softAssertions.assertThat(originalMethod).isNotNull();
		softAssertions.assertThat(originalCallerMethodInDifferentFile).isNotNull();
		softAssertions.assertAll();

		// act
		performRemoveParameter(filesToConsider, fileOfTestClass, lineNumberOfMethodWithParameterToBeRemoved,
				parameterName);

		// assert
		CompilationUnit cuRefactoredFileOfTestClass = JavaParser.parse(fileOfTestClass);
		CompilationUnit cuRefactoredFileWithCallerMethod = JavaParser.parse(fileWithCallerMethod);
		MethodDeclaration refactoredMethod = getMethodByName(TARGET_CLASS_NAME, originalMethod.getNameAsString(),
				cuRefactoredFileOfTestClass);

		// assert that caller method in different file has been refactored
		MethodDeclaration methodInDifferentFileWithTargetMethodCalls = getMethodByName(CALL_OF_TARGET_METHOD_CLASS_NAME,
				originalCallerMethodInDifferentFile.getNameAsString(), cuRefactoredFileWithCallerMethod);
		assertThat(methodInDifferentFileWithTargetMethodCalls).isNotNull();
		assertAllMethodCallsArgumentSizeEqualToRefactoredMethodParameterCount(
				methodInDifferentFileWithTargetMethodCalls, refactoredMethod);
	}

	@Test
	public void testSuperClassRefactored() throws Exception {
		// arrange
		List<File> filesToConsider = new ArrayList<File>();
		filesToConsider.add(fileOfTestClass);
		filesToConsider.add(fileOfSuperClass);
		int lineNumberOfMethodWithParameterToBeRemoved = removeParameterTestClass.getLineOfMethodWithUnusedParameter(0,
				0, 0);
		String parameterName = "b";

		CompilationUnit cuOriginalFileOfSuperClass = JavaParser.parse(fileOfSuperClass);
		MethodDeclaration originalMethodInSuperClass = RefactoringHelper.getMethodDeclarationByLineNumber(
				removeParameterSuperClass.getLineOfMethodWithUnusedParameter(0, 0, 0), cuOriginalFileOfSuperClass);
		assertThat(originalMethodInSuperClass).isNotNull();

		// act
		performRemoveParameter(filesToConsider, fileOfTestClass, lineNumberOfMethodWithParameterToBeRemoved,
				parameterName);

		// assert that target's super class has been refactored
		CompilationUnit cuRefactoredFileOfSuperClass = JavaParser.parse(fileOfSuperClass);
		String methodInSuperClassName = originalMethodInSuperClass.getNameAsString();
		MethodDeclaration methodInSuperClass = getMethodByName(SUPER_CLASS_NAME, methodInSuperClassName,
				cuRefactoredFileOfSuperClass);
		assertThat(methodInSuperClass).isNotNull();
		assertThat(methodInSuperClass.getParameterByName(parameterName).isPresent()).isFalse();
	}

	@Test
	public void testSubClassRefactored() throws Exception {
		// arrange
		List<File> filesToConsider = new ArrayList<File>();
		filesToConsider.add(fileOfTestClass);
		filesToConsider.add(fileOfSubClass);
		int lineNumberOfMethodWithParameterToBeRemoved = removeParameterTestClass.getLineOfMethodWithUnusedParameter(0,
				0, 0);
		String parameterName = "b";

		CompilationUnit cuOriginalFileOfSubClass = JavaParser.parse(fileOfSubClass);
		MethodDeclaration originalMethodInSubClass = RefactoringHelper.getMethodDeclarationByLineNumber(
				removeParameterSubClass.getLineOfMethodWithUnusedParameter(0, 0, 0), cuOriginalFileOfSubClass);
		assertThat(originalMethodInSubClass).isNotNull();

		// act
		performRemoveParameter(filesToConsider, fileOfTestClass, lineNumberOfMethodWithParameterToBeRemoved,
				parameterName);

		// assert that target's sub class has been refactored
		CompilationUnit cuRefactoredFileOfSubClass = JavaParser.parse(fileOfSubClass);
		String methodInSubClassName = originalMethodInSubClass.getNameAsString();
		MethodDeclaration methodInSubClass = getMethodByName(SUB_CLASS_NAME, methodInSubClassName,
				cuRefactoredFileOfSubClass);
		assertThat(methodInSubClass).isNotNull();
		assertThat(methodInSubClass.getParameterByName(parameterName).isPresent()).isFalse();
	}

	@Test
	public void testSiblingClassRefactored() throws Exception {
		// arrange
		List<File> filesToConsider = new ArrayList<File>();
		filesToConsider.add(fileOfTestClass);
		filesToConsider.add(fileOfSiblingClass);
		int lineNumberOfMethodWithParameterToBeRemoved = removeParameterTestClass.getLineOfMethodWithUnusedParameter(0,
				0, 0);
		String parameterName = "b";

		CompilationUnit cuOriginalFileOfSiblingClass = JavaParser.parse(fileOfSiblingClass);
		CompilationUnit cuOriginalFileOfTestClass = JavaParser.parse(fileOfTestClass);
		MethodDeclaration originalMethod = RefactoringHelper.getMethodDeclarationByLineNumber(
				lineNumberOfMethodWithParameterToBeRemoved, cuOriginalFileOfTestClass);
		MethodDeclaration originalMethodInSiblingClass = RefactoringHelper.getMethodDeclarationByLineNumber(
				removeParameterSiblingClass.getLineOfMethodWithUnusedParameter(0, 0, 0), cuOriginalFileOfSiblingClass);
		MethodDeclaration originalCallerMethodInSiblingClass = RefactoringHelper.getMethodDeclarationByLineNumber(
				removeParameterSiblingClass.getLineNumberOfCaller(), cuOriginalFileOfSiblingClass);

		SoftAssertions softAssertions = new SoftAssertions();
		softAssertions.assertThat(originalMethod).isNotNull();
		softAssertions.assertThat(originalMethodInSiblingClass).isNotNull();
		softAssertions.assertThat(originalCallerMethodInSiblingClass).isNotNull();
		softAssertions.assertAll();

		// act
		performRemoveParameter(filesToConsider, fileOfTestClass, lineNumberOfMethodWithParameterToBeRemoved,
				parameterName);

		// assert
		CompilationUnit cuRefactoredFileOfSiblingClass = JavaParser.parse(fileOfSiblingClass);
		CompilationUnit cuRefactoredFileOfTestClass = JavaParser.parse(fileOfTestClass);
		MethodDeclaration refactoredMethod = getMethodByName(TARGET_CLASS_NAME, originalMethod.getNameAsString(),
				cuRefactoredFileOfTestClass);
		
		// assert that target's sibling has been refactored
		String methodInSiblingClassName = originalMethodInSiblingClass.getNameAsString();
		MethodDeclaration methodInSiblingClass = getMethodByName(SIBLING_CLASS_NAME, methodInSiblingClassName,
				cuRefactoredFileOfSiblingClass);
		assertThat(methodInSiblingClass).isNotNull();
		assertThat(methodInSiblingClass.getParameterByName(parameterName).isPresent()).isFalse();
		
		// assert that caller method in target's sibling class has been refactored
		String callerMethodInSiblingClassName = originalCallerMethodInSiblingClass.getNameAsString();
		MethodDeclaration methodInSiblingClassWithSiblingMethodCall = getMethodByName(SIBLING_CLASS_NAME,
				callerMethodInSiblingClassName, cuRefactoredFileOfSiblingClass);
		assertThat(methodInSiblingClassWithSiblingMethodCall).isNotNull();
		assertAllMethodCallsArgumentSizeEqualToRefactoredMethodParameterCount(methodInSiblingClassWithSiblingMethodCall,
				refactoredMethod);
	}

	@Test
	public void testRemoveUsedParameter() throws Exception {
		exception.expect(BotRefactoringException.class);

		// arrange
		List<File> filesToConsider = new ArrayList<File>();
		filesToConsider.add(fileOfTestClass);
		int lineNumberOfMethodWithParameterToBeRemoved = removeParameterTestClass.getLineOfMethodWithUnusedParameter(0,
				0, 0);
		String parameterName = "a";

		// act
		performRemoveParameter(filesToConsider, fileOfTestClass, lineNumberOfMethodWithParameterToBeRemoved,
				parameterName);
	}

	@Test
	public void testRemoveNotExistingParameter() throws Exception {
		exception.expect(BotRefactoringException.class);

		// arrange
		List<File> filesToConsider = new ArrayList<File>();
		filesToConsider.add(fileOfTestClass);
		int lineNumberOfMethodWithParameterToBeRemoved = removeParameterTestClass.getLineOfMethodWithUnusedParameter(0,
				0, 0);
		String parameterName = "d";

		// act
		performRemoveParameter(filesToConsider, fileOfTestClass, lineNumberOfMethodWithParameterToBeRemoved,
				parameterName);
	}

	/**
	 * Tries to remove the parameter from the method in the given line and file.
	 * 
	 * @param filesToConsider
	 *            All files that make up the repository for the specific test
	 * @param targetFile
	 * @param lineNumberOfMethodWithParameterToBeRemoved
	 * @param parameterName
	 * @throws Exception
	 */
	private void performRemoveParameter(List<File> filesToConsider, File targetFile,
			int lineNumberOfMethodWithParameterToBeRemoved, String parameterName) throws Exception {
		GitConfiguration gitConfig = new GitConfiguration();
		gitConfig.setRepoFolder(getAbsolutePathOfTempFolder());

		ArrayList<String> javaRoots = new ArrayList<>();
		javaRoots.add(getAbsolutePathOfTestsFolder());
		BotIssue issue = new BotIssue();
		issue.setFilePath(targetFile.getName());
		issue.setLine(lineNumberOfMethodWithParameterToBeRemoved);
		issue.setJavaRoots(javaRoots);
		issue.setRefactorString(parameterName);
		List<String> allJavaFiles = new ArrayList<>();
		for (File f : filesToConsider) {
			allJavaFiles.add(f.getCanonicalPath());
		}
		issue.setAllJavaFiles(allJavaFiles);

		RemoveMethodParameter refactoring = new RemoveMethodParameter();
		String outputMessage = refactoring.performRefactoring(issue, gitConfig);
		logger.info(outputMessage);
	}

	/**
	 * Asserts that the given parameter is not present in the javadoc of the given
	 * method
	 * 
	 * @param methodDeclaration
	 * @param parameterName
	 */
	private void assertParameterNotPresentInJavadoc(MethodDeclaration methodDeclaration, String parameterName) {
		List<JavadocBlockTag> javadocBlockTags = methodDeclaration.getJavadoc().get().getBlockTags();
		for (JavadocBlockTag javadocBlockTag : javadocBlockTags) {
			if (javadocBlockTag.getTagName().equals("param")) {
				assertThat(javadocBlockTag.getName().get()).isNotEqualTo(parameterName);
			}
		}
	}

	/**
	 * Asserts that all method calls in the body of methodWithTargetMethodCalls have
	 * the same argument size as the refactoredMethod has arguments
	 * 
	 * @param methodWithTargetMethodCalls
	 * @param refactoredMethod
	 */
	private void assertAllMethodCallsArgumentSizeEqualToRefactoredMethodParameterCount(
			MethodDeclaration methodWithTargetMethodCalls, MethodDeclaration refactoredMethod) {
		for (MethodCallExpr methodCall : methodWithTargetMethodCalls.getBody().get().findAll(MethodCallExpr.class)) {
			if (methodCall.getNameAsString().equals(refactoredMethod.getNameAsString())) {
				NodeList<Expression> callerMethodArguments = methodCall.getArguments();
				NodeList<Parameter> refactoredMethodParameters = refactoredMethod.getParameters();

				assertThat(callerMethodArguments).hasSameSizeAs(refactoredMethodParameters);
			}
		}
	}

	/**
	 * TEST HELPER METHOD ONLY. Does not work for classes with with more than one
	 * method declaration with the same name.
	 * 
	 * Finds a method in a compilation unit inside the given class or interface and
	 * with the given name.
	 * 
	 * Hint: this method is needed to find a method in a refacored compilation unit
	 * for which we do not know the line number of the method (removing javadoc
	 * comments changes the lines)
	 * 
	 * @param classOrInterfaceName
	 * @param methodName
	 * @param cu
	 * @return MethodDeclaration or null if none found
	 */
	private MethodDeclaration getMethodByName(String classOrInterfaceName, String methodName, CompilationUnit cu) {
		for (ClassOrInterfaceDeclaration clazz : cu.findAll(ClassOrInterfaceDeclaration.class)) {
			if (clazz.getNameAsString().equals(classOrInterfaceName)) {
				List<MethodDeclaration> methods = clazz.findAll(MethodDeclaration.class);
				for (MethodDeclaration method : methods) {
					if (method.getNameAsString().equals(methodName)) {
						return method;
					}
				}
			}
		}

		return null;
	}

}
