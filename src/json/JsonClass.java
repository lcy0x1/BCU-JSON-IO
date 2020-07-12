package json;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface JsonClass {

	public static enum Type {
		/**
		 * generated from json, requires default constructor, no not allow generate tag
		 */
		DATA,
		/** generated by holder class, requires generator tag */
		FILL,
		/** generated from json, requires generator method with parameter JsonObject */
		MANUAL, ALLDATA
	}

	String generator() default "";

	JsonClass.Type type();

}