package json;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * ways to read from JSON: <br>
 * 1. default setting, use on fields, put {@code @JsonField}, ignore fields not
 * in JSON <br>
 * 2. customize IOType, to be used only when reading or writing, add parameter
 * {@code IOType} <br>
 * 3. use on methods, must have {@code IOType} {@code R} or {@code W} <br>
 * 4. {@code GenType} {@code FILL} mode, on {@code JsonClassType} {@code SET} or
 * {@code FILL} object field only, allows modification on pre-existing objects
 * 5. {@Code GenType} {@code GEN} mode, use parameter {@code generator} to
 * specify function name, must be static function declared in this class
 */
@Documented
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface JsonField {

	public static enum GenType {
		SET, FILL, GEN
	}

	public static enum IOType {
		R, W, RW
	}

	/**
	 * ignored when GenType is not GEN, must refer to a static method declared in
	 * this class with parameter of this type and {@code JsonObject}. second
	 * parameter can be unused, as it will also be injected
	 */
	String generator() default "";

	/**
	 * Generation Type for this Field. Default is SET, which means to set the value.
	 * FILL requires a default value and must be used on object fields. GEN uses
	 * generator function. Functional Fields must use SET.
	 */
	GenType GenType() default GenType.SET;

	IOType IOType() default IOType.RW;

	boolean noErr() default false;

	/**
	 * tag name for this field, use the field name if not specified. Must be
	 * specified for functions
	 */
	String tag() default "";

}