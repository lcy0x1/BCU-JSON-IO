package main;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

import json.JsonClass;
import json.JsonField;
import json.JsonClass.Type;

/** not going to use it anytime soon */
@SupportedAnnotationTypes({ "JsonClass", "JsonField" })
public class JsonAnnoProc extends AbstractProcessor {

	@Override
	public boolean process(Set<? extends TypeElement> set, RoundEnvironment env) {
		for (TypeElement type : set) {
			JsonClass jc0 = type.getAnnotation(JsonClass.class);
			if (jc0 == null)
				continue;
			if (type.getKind() != ElementKind.CLASS) {
				processingEnv.getMessager().printMessage(Kind.ERROR, "JsonClass can only be used for class", type);
				continue;
			}
			TypeMirror mir = type.getSuperclass();
			if (!(mir instanceof NoType)) {
				JsonClass jc1 = mir.getAnnotation(JsonClass.class);
				if (jc1 != null && jc1.type() != JsonClass.Type.DATA)
					processingEnv.getMessager().printMessage(Kind.ERROR,
							"the superclass of JsonClass, if is also JsonClass, have to be Type DATA", type);
			}
			String cgen = "";
			if (jc0.type() == Type.MANUAL) {
				cgen = jc0.generator();
				if (cgen.length() == 0)
					processingEnv.getMessager().printMessage(Kind.ERROR,
							"MANUAL type have to have a generator parameter", type);
			}
			Map<String, Element> gen = new HashMap<>();
			for (Element e : type.getEnclosedElements()) {
				if (e.getKind() == ElementKind.FIELD && e.getAnnotation(JsonField.class) != null) {
					JsonField jf = e.getAnnotation(JsonField.class);
					if (jf.generator().length() > 0)
						gen.put(jf.generator(), e);
				}
			}
			for (Element e : type.getEnclosedElements()) {
				if (e.getKind() == ElementKind.METHOD && e.getModifiers().contains(Modifier.STATIC)) {
					String name = e.getSimpleName().toString();
					ExecutableElement ee = (ExecutableElement) e;
					List<? extends VariableElement> list = ee.getParameters();
					if (list.size() != 1)
						continue;
					TypeMirror param = list.get(0).asType();
					TypeMirror ret = ee.getReturnType();
					Elements elem = processingEnv.getElementUtils();
					Types types = processingEnv.getTypeUtils();
					TypeMirror jobj = elem.getTypeElement("com.google.gson.JsonObject").asType();
					if (cgen == name && types.isAssignable(jobj, param) && types.isAssignable(ret, type.asType()))
						cgen = "";
					if (gen.get(name) != null && types.isAssignable(type.asType(), param)
							&& types.isAssignable(ret, gen.get(name).asType()))
						gen.remove(name);
				}
			}
			if (cgen.length() > 0)
				processingEnv.getMessager().printMessage(Kind.ERROR,
						"Generator not found. <br>" + "Generator function should be static. <br>"
								+ "Generator must have and only have parameter type JsonObject. <br>"
								+ "Generator must have return type as this class type.",
						type);
			for (Entry<String, Element> ent : gen.entrySet())
				processingEnv.getMessager().printMessage(Kind.ERROR,
						"Generator not found. <br>"
								+ "Generator must have and only have parameter type as this class type. <br>"
								+ "Generator must have return type as the type of this field.",
						ent.getValue());
		}

		return false;
	}

}
