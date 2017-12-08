package pwr.chrzescijanek.filip.higseg.inject;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

/**
 * Provides methods for class instantiation using singleton components pool.
 */
public class Injector {

	private static final Logger LOGGER = Logger.getLogger(Injector.class.getName());

	private static final Map<Class<?>, Object> COMPONENTS = new WeakHashMap<>();

	private static final Function<Class<?>, Object> SUPPLIER = createInstanceSupplier();

	private Injector() { }

	private static Function<Class<?>, Object> createInstanceSupplier() {
		return (c) -> {
			try {
				final List<? extends Constructor<?>> constructors = getConstructors(c);
				checkForTooManyConstructors(c, constructors);
				if (constructors.isEmpty()) return c.newInstance();
				return createInstance(constructors.get(0));
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
				final IllegalStateException ex = new IllegalStateException("Could not instantiate: " + c, e);
				LOGGER.log(Level.SEVERE, ex.toString(), ex);
				throw ex;
			}
		};
	}

	private static List<? extends Constructor<?>> getConstructors(final Class<?> c) {
		return stream(c.getConstructors()).filter(r -> r.isAnnotationPresent(Inject.class))
		                                  .collect(toList());
	}

	private static void checkForTooManyConstructors(final Class<?> c, final List<? extends Constructor<?>>
			constructors) throws
	                      InstantiationException {
		if (constructors.size() > 1) {
			final InstantiationException ex = new InstantiationException(
					String.format("Found more than one constructor annotated with @Inject in %s class", c.getName())
			);
			LOGGER.log(Level.SEVERE, ex.toString(), ex);
			throw ex;
		}
	}

	private static Object createInstance(final Constructor<?> constructor) throws InstantiationException,
	                                                                              IllegalAccessException,
	                                                                              InvocationTargetException {
		final Object[] values = instantiateParameters(constructor);
		return constructor.newInstance(values);
	}

	private static Object[] instantiateParameters(final Constructor<?> constructor) {
		final Parameter[] parameters = constructor.getParameters();
		return stream(parameters)
				.filter(parameter -> checkIfNotPrimitiveOrString(parameter.getType()))
				.map(parameter -> instantiateComponent(parameter.getType()))
				.toArray();
	}

	/**
	 * @param clazz class
	 * @param <T>   type
	 * @return instance of given class
	 */
	public static <T> T instantiate(final Class<T> clazz) {
		final
		T instance = injectFields((T) SUPPLIER.apply(clazz));
		return instance;
	}

	/**
	 * Instantiates class and, if needed, adds the new instance to singleton component pool.
	 *
	 * @param clazz class
	 * @param <T>   type
	 * @return instance of given class
	 */
	public static <T> T instantiateComponent(final Class<T> clazz) {
		T product = (T) COMPONENTS.get(clazz);
		if (product == null) {
			product = injectFields((T) SUPPLIER.apply(clazz));
			COMPONENTS.put(clazz, product);
		}
		return clazz.cast(product);
	}

	/**
	 * Clears singleton component pool.
	 */
	public static void reset() {
		COMPONENTS.clear();
	}

	private static <T> T injectFields(final T instance) {
		injectFields(instance.getClass(), instance);
		return instance;
	}

	private static void injectFields(final Class<?> clazz, final Object instance) {
		final Field[] fields = clazz.getDeclaredFields();
		stream(fields)
				.filter(field -> field.isAnnotationPresent(Inject.class))
				.filter(field -> checkIfNotPrimitiveOrString(field.getType()))
				.forEach(field -> {
					final Object value = instantiateComponent(field.getType());
					if (Objects.nonNull(value))
						injectField(field, instance, value);
				});
		final Class<?> superclass = clazz.getSuperclass();
		if (Objects.nonNull(superclass))
			injectFields(superclass, instance);
	}

	private static boolean checkIfNotPrimitiveOrString(final Class<?> type) {
		return !type.isPrimitive() && !type.isAssignableFrom(String.class);
	}

	private static void injectField(final Field field, final Object instance, final Object value) {
		AccessController.doPrivileged((PrivilegedAction<?>) () -> {
			final boolean wasAccessible = field.isAccessible();
			try {
				field.setAccessible(true);
				field.set(instance, value);
				return null;
			} catch (IllegalArgumentException | IllegalAccessException e) {
				final IllegalStateException ex =
						new IllegalStateException(
								String.format("Could not set value %s to field %s", value, field), e);
				LOGGER.log(Level.SEVERE, ex.toString(), ex);
				throw ex;
			} finally {
				try {
					field.setAccessible(wasAccessible);
				} catch (final SecurityException e) {
					LOGGER.log(Level.SEVERE, e.toString(), e);
				}
			}
		});
	}
}
