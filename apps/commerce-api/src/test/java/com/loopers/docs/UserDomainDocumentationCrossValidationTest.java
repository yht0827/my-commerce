package com.loopers.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.interfaces.api.user.UserDto;
import com.loopers.interfaces.api.user.UserV1Controller;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

class UserDomainDocumentationCrossValidationTest {

	private static final String REQUIREMENTS_DOC = "01-requirements.md";
	private static final String SEQUENCE_DOC = "02-sequence-diagrams.md";
	private static final String CLASS_DIAGRAM_DOC = "03-class-diagrams.md";
	private static final String ERD_DOC = "04-erd.md";

	@Test
	@DisplayName("요구사항/시퀀스 문서의 User API 경로는 컨트롤러 매핑과 일치한다.")
	void userApiPathsShouldMatchControllerMappings() throws NoSuchMethodException {
		RequestMapping classMapping = UserV1Controller.class.getAnnotation(RequestMapping.class);
		String basePath = classMapping.value()[0];

		Method createMethod = UserV1Controller.class.getMethod("createUser", UserDto.V1.CreateUserRequest.class);
		PostMapping postMapping = createMethod.getAnnotation(PostMapping.class);
		String createUserPath = resolvePath(basePath, postMapping.value(), postMapping.path());

		Method getMethod = UserV1Controller.class.getMethod("getUser", String.class);
		GetMapping getMapping = getMethod.getAnnotation(GetMapping.class);
		String getUserPath = resolvePath(basePath, getMapping.value(), getMapping.path());

		String requirementsDoc = readDoc(REQUIREMENTS_DOC);
		String sequenceDoc = readDoc(SEQUENCE_DOC);

		assertThat(requirementsDoc).contains(createUserPath, getUserPath, "X-USER-ID");
		assertThat(sequenceDoc).contains(createUserPath, getUserPath, "X-USER-ID");
	}

	@Test
	@DisplayName("요구사항 문서의 User 응답 필드는 실제 응답 DTO와 일치한다.")
	void requirementsResponseFieldsShouldMatchUserResponse() {
		String requirementsDoc = readDoc(REQUIREMENTS_DOC);

		List<String> responseFieldNames = Arrays.stream(UserDto.V1.UserResponse.class.getRecordComponents())
			.map(RecordComponent::getName)
			.toList();

		for (String fieldName : responseFieldNames) {
			assertThat(requirementsDoc).contains("| " + fieldName + " |");
		}
	}

	@Test
	@DisplayName("ERD 문서의 users 테이블 정의는 User 엔티티/제약조건과 일치한다.")
	void erdUsersTableShouldMatchUserEntityAndConstraints() {
		String erdDoc = readDoc(ERD_DOC);

		Table table = User.class.getAnnotation(Table.class);
		assertThat(erdDoc).contains("### " + table.name());

		List<String> expectedColumnNames = List.of(
			embeddedColumnName("userId"),
			embeddedColumnName("email"),
			embeddedColumnName("birthday"),
			"gender",
			baseEntityColumnName("createdAt"),
			baseEntityColumnName("updatedAt"),
			baseEntityColumnName("deletedAt")
		);

		for (String columnName : expectedColumnNames) {
			assertThat(erdDoc).contains("| " + columnName + " |");
		}

		assertThat(erdDoc).contains(
			privateStaticString(UserService.class, "USER_ID_UNIQUE_CONSTRAINT"),
			privateStaticString(UserService.class, "USER_EMAIL_UNIQUE_CONSTRAINT")
		);
	}

	@Test
	@DisplayName("클래스 다이어그램 문서는 User 용어를 사용한다.")
	void classDiagramShouldUseUserTerminology() {
		String classDiagramDoc = readDoc(CLASS_DIAGRAM_DOC);
		assertThat(classDiagramDoc).contains("## 회원 (User)", "class User");
		assertThat(classDiagramDoc).doesNotContain("class Member");
	}

	private static String resolvePath(final String basePath, final String[] values, final String[] paths) {
		String subPath = firstPath(values, paths);
		if (subPath.isEmpty()) {
			return basePath;
		}
		if (basePath.endsWith("/") && subPath.startsWith("/")) {
			return basePath + subPath.substring(1);
		}
		if (!basePath.endsWith("/") && !subPath.startsWith("/")) {
			return basePath + "/" + subPath;
		}
		return basePath + subPath;
	}

	private static String firstPath(final String[] values, final String[] paths) {
		if (values != null && values.length > 0 && !values[0].isBlank()) {
			return values[0];
		}
		if (paths != null && paths.length > 0 && !paths[0].isBlank()) {
			return paths[0];
		}
		return "";
	}

	private static String embeddedColumnName(final String entityFieldName) {
		try {
			Field field = User.class.getDeclaredField(entityFieldName);
			return Arrays.stream(field.getType().getDeclaredFields())
				.map(candidate -> candidate.getAnnotation(Column.class))
				.filter(Objects::nonNull)
				.map(Column::name)
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Embedded column not found: " + entityFieldName));
		} catch (NoSuchFieldException e) {
			throw new IllegalStateException("Entity field not found: " + entityFieldName, e);
		}
	}

	private static String baseEntityColumnName(final String fieldName) {
		try {
			Field field = BaseEntity.class.getDeclaredField(fieldName);
			Column column = field.getAnnotation(Column.class);
			if (column == null) {
				throw new IllegalStateException("Column annotation not found in BaseEntity: " + fieldName);
			}
			return column.name();
		} catch (NoSuchFieldException e) {
			throw new IllegalStateException("BaseEntity field not found: " + fieldName, e);
		}
	}

	private static String privateStaticString(final Class<?> type, final String fieldName) {
		try {
			Field field = type.getDeclaredField(fieldName);
			field.setAccessible(true);
			return (String) field.get(null);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new IllegalStateException("Cannot read constant: " + type.getSimpleName() + "." + fieldName, e);
		}
	}

	private static String readDoc(final String fileName) {
		Path docsDir = locateRepositoryRoot().resolve("docs");
		Path docPath = docsDir.resolve(fileName);
		try {
			return Files.readString(docPath);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot read doc file: " + docPath, e);
		}
	}

	private static Path locateRepositoryRoot() {
		Path current = Path.of("").toAbsolutePath();
		while (current != null) {
			boolean hasSettings = Files.isRegularFile(current.resolve("settings.gradle.kts"));
			boolean hasDocs = Files.isRegularFile(current.resolve("docs/01-requirements.md"));
			if (hasSettings && hasDocs) {
				return current;
			}
			current = current.getParent();
		}
		throw new IllegalStateException("Repository root not found from current working directory.");
	}
}
