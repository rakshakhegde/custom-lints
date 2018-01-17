package com.example.lint.checks;

import com.android.tools.lint.client.api.UElementHandler;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.UastScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.PsiType;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.ULocalVariable;
import org.jetbrains.uast.UQualifiedReferenceExpression;
import org.jetbrains.uast.UReturnExpression;
import org.jetbrains.uast.UastUtils;

/**
 * Created by rakshak_cont on 12/01/18.
 */

public class RxUsedEnforcerJ extends Detector implements UastScanner {

	static final Issue ISSUE = Issue.create(
			RxUsedEnforcerJ.class.getSimpleName(),
			"Make sure function returning Rx type is being used (typically `subscribe()`)",
			"",
			Category.CORRECTNESS,
			10,
			Severity.ERROR,
			new Implementation(RxUsedEnforcerJ.class, Scope.JAVA_FILE_SCOPE)
	);
	private static Stream<String> RX_PRIMITIVE_CANONICAL_NAMES = Stream.of(
			"io.reactivex.Observable",
			"io.reactivex.Single",
			"io.reactivex.Completable",
			"io.reactivex.Maybe",
			"io.reactivex.Flowable"
	);

	@Nullable
	@Override
	public List<Class<? extends UElement>> getApplicableUastTypes() {
		return Collections.singletonList(UCallExpression.class);
	}

	@Nullable
	@Override
	public UElementHandler createUastHandler(JavaContext context) {
		System.out.println(UastUtils.asRecursiveLogString(context.getUastFile()));

		return new UElementHandler() {
			@Override
			public void visitCallExpression(UCallExpression node) {
				PsiType returnType = node.getReturnType();

				if (returnType == null) {
					return;
				}

				boolean returnTypeStartsWithAnyRx = RX_PRIMITIVE_CANONICAL_NAMES
						.anyMatch(rxClassName -> returnType.getCanonicalText().startsWith(rxClassName));

				boolean valid = returnTypeStartsWithAnyRx &&
						!(node.getUastParent() instanceof UQualifiedReferenceExpression) &&
						!(node.getUastParent() instanceof ULocalVariable) &&
						!(node.getUastParent() instanceof UReturnExpression);

				System.out.println(
						"reporting from visitCallExpression => " + node + " => " + node.getUastParent().getClass().getCanonicalName() + " => " + returnType
								.getCanonicalText() + " => valid=" + valid);
				if (valid) {
					context.report(ISSUE,
							context.getCallLocation(node, false, false),
							"Make use of the returned Rx type"
					);
				}
			}
		};

	}
}
