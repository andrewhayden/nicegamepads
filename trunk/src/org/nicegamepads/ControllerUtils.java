package org.nicegamepads;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Rumbler;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Controller.Type;

/**
 * Provides static utilities for accessing and interacting with controllers.
 * <p>
 * Most of the methods in this class will cache their results because of
 * the simple fact that controllers don't change their composition at
 * runtime.  
 * 
 * @author Andrew Hayden
 */
public final class ControllerUtils
{
    /**
     * Unlikely soft limit on how many controllers we can cache data for.
     * Could be breached in multithreaded environment but we don't really care
     * since this is probably never going to happen anyhow.
     * <p>
     * This is just a sanity bound in case something goes berserk.
     */
    private static volatile int maxCacheSizeSoftLimit = 10000;

    /**
     * If a QWERTY keyboard is hanging around it probably has a "component" for
     * each letter, since there are 26 of them it would be extremely weird for
     * it to be any other way (possibly with the exception of a digital
     * control with many distinct values).
     */
    private final static Set<String> PROBABLE_ENGLISH_KEYBOARD_COMPONENT_NAMES =
        Collections.unmodifiableSet(
                new HashSet<String>(Arrays.asList(new String[] {
                    "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K",
                    "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
                    "W", "X", "Y", "Z"
        })));

    /**
     * Even most non-US keyboards have function keys.  In a brief survey of
     * about 20 keyboards online, all had "F1", "F2", "F3" (etc) regardless
     * of the language or usual OS platform (e.g., Macintosh-branded x86 versus
     * generic x86 hardware).  Furthermore in every case I have seen, the
     * function keys are actually labeled with a Latin-style letter "F".
     * <p>
     * The strings are declared here so they will be in Java's internal
     * modified-UTF-16 representation at runtime.
     */
    private final static Set<String> PROBABLE_ANY_KEYBOARD_COMPONENT_NAMES =
        Collections.unmodifiableSet(
                new HashSet<String>(Arrays.asList(new String[] {
                    "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8"
        })));

    /**
     * Cache of computed type codes for controllers we have seen this run.
     */
    private final static Map<Controller, Integer> cachedTypeCodesByController
        = Collections.synchronizedMap(new HashMap<Controller, Integer>());

    /**
     * Cache of computed type codes for components we have seen this run.
     */
    private final static Map<Component, Integer> cachedTypeCodesByComponent
        = Collections.synchronizedMap(new HashMap<Component, Integer>());

    /**
     * Cache of subcomponents by controller (non-recursive).
     */
    private final static Map<Controller, List<Component>>
        cachedComponentsByController
            = Collections.synchronizedMap(
                    new HashMap<Controller, List<Component>>());

    /**
     * Cache of subcomponents by controller (recursively expanded).
     */
    private final static Map<Controller, List<Component>>
        cachedDeepComponentsByController
            = Collections.synchronizedMap(
                    new HashMap<Controller, List<Component>>());

    /**
     * Cache of whether or not controllers are gamepad-like.
     */
    private final static Map<Controller, Boolean>
        cachedGamepadLikeResultsByController =
            Collections.synchronizedMap(new HashMap<Controller, Boolean>());

    /**
     * Cache of reverse-lookups for finding the parent controller of a
     * component.
     */
    private final static Map<Component, Controller>
        cachedParentControllersByComponent =
            Collections.synchronizedMap(new HashMap<Component, Controller>());

    /**
     * Private constructor discourages unwanted instantiation.
     */
    private ControllerUtils()
    {
        // Private constructor discourages unwanted instantiation.
    }

    /**
     * Generates a type code for the type of component specified.
     * <p>
     * The type code should be the same every time the same component is
     * present, even between runs, provided that there are no changes to
     * drivers or jinput libraries.  It should also be the same regardless
     * of which individual <em>phsyical controller</em> is plugged in so
     * long as it is identical to others of its type.  For example, if you
     * plug in one XBOX360 controller, its buttons should have the same
     * generic type codes as the buttons of every other XBOX360 controller
     * of the same hardware revision.
     * <p>
     * Note that this is <em>not</em> a hashcode.  It does <em>not</em>
     * uniquely identify an individual piece of hardware.
     * <p>
     * This method lazily caches type codes so that subsequent lookups are
     * (very) cheap for the same physical device in any given run.
     * 
     * @param component the component to generate a type code for
     * @return the type code for this type of component
     */
    final static int generateTypeCode(Component component)
    {
        Integer cached = cachedTypeCodesByComponent.get(component);
        if (cached == null)
        {
            int typeCode = 39;
            String name = component.getName();
            boolean isAnalog = component.isAnalog();
            boolean isRelative = component.isRelative();
            typeCode += 39 * (name == null? 0 : name.hashCode());
            typeCode += 39 * (isAnalog ? 11 : 17);
            typeCode += 39 * (isRelative ? 13 : 19);

            if (cachedTypeCodesByComponent.size() < maxCacheSizeSoftLimit)
            {
                cachedTypeCodesByComponent.put(component, typeCode);
            }
            return typeCode;
        }
        return cached;
    }

    /**
     * Generates a type code for the type of controller specified.
     * <p>
     * The type code should be the same every time the same controller is
     * present, even between runs, provided that there are no changes to
     * drivers or jinput libraries.  It should also be the same regardless
     * of which individual <em>phsyical controller</em> is plugged in so
     * long as it is identical to others of its type.  For example, if you
     * plug in one XBOX360 controller, it should have the same generic type
     * code as every other XBOX360 controller of the same hardware revision.
     * <p>
     * Note that this is <em>not</em> a hashcode.  It does <em>not</em>
     * uniquely identify an individual piece of hardware.
     * <p>
     * This method lazily caches type codes so that subsequent lookups are
     * (very) cheap for the same physical device in any given run.
     * 
     * @param controller the controller to generate a type code for
     * @return the type code for the type of controller
     */
    final static int generateTypeCode(Controller controller)
    {
        // Lazy caching.  Always computes the same value so no need for
        // extra thread safety.
        Integer cached = cachedTypeCodesByController.get(controller);
        if (cached == null)
        {
            int typeCode = 37;
            String controllerName = controller.getName();
            typeCode += 37 * (controllerName == null?
                    0 : controllerName.hashCode());

            for (Component component : controller.getComponents())
            {
                typeCode += 37 * generateTypeCode(component);
            }

            for (Rumbler rumbler : controller.getRumblers())
            {
                String associatedAxisName = rumbler.getAxisName();
                typeCode += 1 + (37 * (associatedAxisName == null ?
                        0 : associatedAxisName.hashCode()));
            }

            // Descend into subcontrollers.
            Controller[] subControllers = controller.getControllers();
            typeCode += 37 * subControllers.length;
            for (Controller subController : subControllers)
            {
                typeCode += 37 * generateTypeCode(subController);
            }

            if (cachedTypeCodesByController.size() < maxCacheSizeSoftLimit)
            {
                cachedTypeCodesByController.put(controller, typeCode);
            }
            return typeCode;
        }
        else
        {
            return cached;
        }
    }

    /**
     * Returns the first gamepad found on the system.
     * <p>
     * This method scans the system for all known gamepads (or gamepad-like
     * devices, if requested) and returns the first one found.
     * 
     * @param strictGamepadMatching if <code>true</code>, only the first
     * gamepad that passes {@link #isGamepad(Controller)} will be returned;
     * otherwise, the first device that passes
     * {@link #isGamepadLike(Controller)} will be returned. 
     * @return the first gamepad (or gamepad-like device, if requested) found,
     * if any; otherwise, <code>null</code>
     */
    public final static Controller getDefaultGamepad(
            boolean strictGamepadMatching)
    {
        List<Controller> allGamepads = getAllGamepads(strictGamepadMatching);
        if (allGamepads.size() > 0)
        {
            return allGamepads.get(0);
        }
        return null;
    }

    /**
     * Returns a list of all controllers regardless of their type.
     * 
     * @return such a list, possibly of length 0 but never <code>null</code>
     */
    public final static List<Controller> getAllControllers()
    {
        return Arrays.asList(
                ControllerEnvironment.getDefaultEnvironment().getControllers());
    }

    /**
     * Returns the type of the component.
     * <p>
     * This type is independent of jinput and is based upon a JDK 5+
     * enumeration, which is more efficient than the run-time type casting
     * that would otherwise be necessary to determine what the type of the
     * component is.  Since component types cannot change at runtime,
     * callers are strongly encouraged to cache the return value.
     * <p>
     * If the type is unrecognized, throws a {@link RuntimeException}.
     * 
     * @param component the component to check
     * @return the type of the component
     */
    public final static ComponentType getComponentType(Component component)
    {
        Identifier identifier = component.getIdentifier();
        if (identifier instanceof Identifier.Axis)
        {
            return ComponentType.AXIS;
        }
        if (identifier instanceof Identifier.Button)
        {
            return ComponentType.BUTTON;
        }
        if (identifier instanceof Identifier.Key)
        {
            return ComponentType.KEY;
        }

        throw new RuntimeException(
                "Unsupported jinput component type: " + identifier);
    }

    /**
     * Returns the cached information about the parent controller of the
     * specified component.
     * <p>
     * This information is only available if
     * {@link #cacheControllerInfo(Controller, boolean)} has been called
     * at some point in the past for a controller that contained the
     * component.
     * 
     * @param component the component to look up the parent of
     * @return the parent controller of this component, if known; otherwise,
     * <code>null</code>
     */
    public final static Controller getCachedParentController(
            Component component)
    {
        return cachedParentControllersByComponent.get(component);
    }

    /**
     * Forces the system to cache the information for the specified controller,
     * including child-to-parent relationships between components and
     * controllers.
     * <p>
     * This method does the minimum amount of work necessary to perform
     * the caching.  If all information is already cached, this method
     * completes quickly.
     * 
     * @param controller the controller to cache information about
     * @param deep whether or not to recursively descend into subcontrollers
     * as well
     */
    public final static void cacheControllerInfo(
            Controller controller, boolean deep)
    {
        // Just call getComponents.
        getComponents(controller, deep);
    }

    /**
     * Returns an unmodifiable listing of all of the components of the
     * specified controller (and, optionally, any subcontrollers
     * contained therein).
     * <p>
     * It is often unimportant under which subcontroller a component resides,
     * so long as the component can be found and configured.  To that end,
     * this method provides a way to "flatten" the controller space and simply
     * get a list of every component in the entire controller, regardless of
     * any nested subcontrollers that may be present in the device
     * (for example, an integrated mouse in a keyboard).
     * <p>
     * Since controllers cannot gain or lose components at runtime, this method
     * caches results.  Subsequent calls will generally be much faster.
     * 
     * @param controller the controller to find the components of
     * @param recursive whether or not to recursively descend into any and all
     * subcontrollers
     * @return an unmodifiable list of all the components in the controller
     */
    public final static List<Component> getComponents(
            Controller controller, boolean recursive)
    {
        List<Component> results = null;
        if(!recursive)
        {
            results = cachedComponentsByController.get(controller);
            if (results == null)
            {
                // Calculate and cache.
                results = new ArrayList<Component>();
                getComponentsHelper(controller, results);
                if (cachedComponentsByController.size() < maxCacheSizeSoftLimit)
                {
                    cachedComponentsByController.put(
                            controller, Collections.unmodifiableList(results));
                }
            }
        }
        else
        {
            results = cachedDeepComponentsByController.get(controller);
            if (results == null)
            {
                // Calculate and cache.
                results = new ArrayList<Component>();
                for (Component component : controller.getComponents())
                {
                    results.add(component);
                }
                if (cachedDeepComponentsByController.size() < maxCacheSizeSoftLimit)
                {
                    cachedDeepComponentsByController.put(
                            controller, Collections.unmodifiableList(results));
                }
            }
        }

        return results;
    }

    /**
     * Recursively retrieves all components for the specified controller,
     * in a breadth-first search of the controller space.
     * 
     * @param controller the controller to read from
     * @param results running list of all components found so far
     */
    private final static void getComponentsHelper(Controller controller,
            List<Component> results)
    {
        for (Component component : controller.getComponents())
        {
            if (cachedParentControllersByComponent.get(component) == null
                    && cachedParentControllersByComponent.size() < maxCacheSizeSoftLimit)
            {
                cachedParentControllersByComponent.put(component, controller);
            }
            results.add(component);
        }

        for (Controller subController : controller.getControllers())
        {
            getComponentsHelper(subController, results);
        }
    }

    /**
     * Returns all of the gamepads or gamepad-like devices attached to the
     * system (as directed).
     * <p>
     * This method scans the system for all controllers, locates those that
     * are probably gamepads (or gamepad-like devices, if requested), and
     * returns them.
     * <p>
     * For details on exactly what constitutes a gamepad-like device,
     * see {@link #isGamepadLike(Controller)}.
     * 
     * @param strictGamepadMatching if <code>true</code>, only gamepads that
     * pass {@link #isGamepad(Controller)} will be returned; otherwise,
     * all devices that pass {@link #isGamepadLike(Controller)} will be
     * returned. 
     * @return a list of all gamepads (or gamepad-like devices, if requested)
     * attached to the system, psosibly of length zero but never
     * <code>null</code>
     */
    public final static List<Controller> getAllGamepads(
            boolean strictGamepadMatching)
    {
        List<Controller> allGamepads = new ArrayList<Controller>();
        Controller[] allControllers =
            ControllerEnvironment.getDefaultEnvironment().getControllers();
        if (allControllers != null)
        {
            for (Controller controller : allControllers)
            {
                if (strictGamepadMatching)
                {
                    if (isGamepad(controller))
                    {
                        allGamepads.add(controller);
                    }
                }
                else
                {
                    if (isGamepadLike(controller))
                    {
                        allGamepads.add(controller);
                    }
                }
            }
        }
        return allGamepads;
    }

    /**
     * Returns whether or not the specific controller is a gamepad in the
     * strictest possible sense.
     * <p>
     * This method asks the device what type it is, and if it responds with
     * the appropriate type for Gamepad (as defined by jinput), then
     * it is considered a gamepad and the return value is <code>true</code>.
     * In all other cases the return value is <code>false</code>.
     * <p>
     * In principle this is the way to make the determination.  In reality,
     * there are many devices manufactured that do not comply with this
     * highly-desirable proposition.  Depending on how you wish to use
     * the system, you may want to consider testing whether the device is
     * <em>gamepad-like</em> instead.  See {@link #isGamepadLike(Controller)}
     * for more information.
     * 
     * @param controller the controller to test
     * @return <code>true</code> if the controller claims to be a gamepad
     * explicitly and precisely; otherwise, <code>false</code>
     * @see #isGamepadLike(Controller)
     */
    public final static boolean isGamepad(Controller controller)
    {
        return (controller.getType() == Type.GAMEPAD);
    }

    /**
     * Returns whether or not this device is likely a gamepad or a gamepad-
     * like device.
     * <p>
     * Essentially, a device is considered gamepad-like if it probably isn't
     * a keyboard and probably isn't a mouse.  This lumps together things
     * like wheels, rudders, joysticks, trackballs and gamepads as being
     * "gamepad-like" in that they all may serve a primary purpose that is
     * unrelated to the typical usage mode for a modern computing system.
     * This method may, in unusual circumstances, be incorrect.
     * <p>
     * The rules governing this process are fairly complex, and are described
     * in terms of a filter as follows:
     * <ol>
     * <li>Ask the controller what "type" it claims to be.</li>
     * <li>If it claims to be a gamepad, assume that it isn't lying and
     *     return <code>true</code>.</li>
     * <li>If it claims to be a keyboard or a mouse, assume that it isn't
     *     lying and return <code>false</code>.</li>
     * <li>Ask the controller what it's "name" is and convert it to uppercase
     *     in the locale of the host.</li>
     * <li>If the name contains the substring "KEYBOARD" or "MOUSE" assume
     *     that the device is a keyboard or mouse respectively and return
     *     <code>false</code>.</li>
     * <li>Check if it has rumblers.  If it does, assume that no sane
     *     manufacturer includes a rumbler in a keyboard or mouse and
     *     return <code>true</code>.</li>
     * <li>Get the names of every single component in the controller and
     *     convert to uppercase in the locale of the host.</li>
     * <li>If the names are a superset of all 26 English letters, assume
     *     it is a keyboard and return <code>false</code>.</li?
     * <li>If the names are a superset of {F1,F2,F3,F4,F5,F6,F7,F8}
     *     <em>and</em> there are at least 50 components in the controller,
     *     assume it is a non-English keyboard and return
     *     <code>false</code>.</li>
     * <li>Anything making it this far is probably not a keyboard nor a mouse,
     *     so return <code>true</code>.</li>
     * </ol>
     * <p>
     * Because the logic for determining whether or not a controller is
     * gamepad-like is potentially complex and expensive to compute, it is
     * cached.  Subsequent calls will generally be much faster.
     * 
     * @param controller the controller to test
     * @return <code>true</code> if it is very likely that this controller
     * is a gamepad or gamepad-like device; otherwise, <code>false</code>
     */
    public final static boolean isGamepadLike(Controller controller)
    {
        // First, consult cache.  If we already know the answer, return it.
        Boolean cachedResult =
            cachedGamepadLikeResultsByController.get(controller);
        if (cachedResult != null)
        {
            return cachedResult;
        }

        // If the answer isn't cached, calculate and return.
        boolean isGamepadLike = isGamepadLikeInternal(controller);
        if (cachedGamepadLikeResultsByController.size() < maxCacheSizeSoftLimit)
        {
            cachedGamepadLikeResultsByController.put(controller, isGamepadLike);
        }
        return isGamepadLike;
    }

    /**
     * Calculated whether or not the controller is gamepad-like.
     * 
     * @param controller the controller to test
     * @return <code>true</code> if the controller is gamepad-like;
     * otherwise, <code>false</code>
     * @see #isGamepadLike(Controller)
     */
    private final static boolean isGamepadLikeInternal(Controller controller)
    {
        // First we'll check some common types.  These can lie, though,
        // depending on the manufacturer, so we're only going to give them
        // a brief look.
        Type controllerType = controller.getType();
        if (controllerType == Type.GAMEPAD)
        {
            // Claims to be a gamepad.  This seems good enough to me.
            return true;
        }

        // Not a gamepad by declaration.  Is it declared as a keyboard or
        // mouse?
        if (controllerType == Type.KEYBOARD
                || controllerType == Type.MOUSE)
        {
            // Definitely reporting itself as a keyboard or a mouse.
            // No arguments here!
            return false;
        }

        // OK, so it doesn't have a type we can filter.  What about its name?
        // Let's convert it to uppercase in the host's locale and see if
        // it contains "KEYBOARD" or "MOUSE"... a non-English device might
        // contain a different word, but we can't have a dictionary of
        // every language's definition for "keyboard" or "mouse" here.
        // We'll try to catch those later on with heuristics.
        String controllerName = controller.getName();
        if (controllerName != null)
        {
            String uppercase = controllerName.toUpperCase();
            if (uppercase.contains("KEYBOARD")
                    || uppercase.contains("MOUSE"))
            {
                // The device name says that it is a mouse or a keyboard.
                // This seems to indicate that it isn't a gamepad.
                return false;
            }
        }

        // ====================================================================
        // NON-DETERMINISTIC SECTION FOLLOWS
        // ====================================================================
        // In my experience, anything that is not a keyboard or a mouse
        // is potentially a game controller.  This could be because the
        // manufacturer got lazy or because the controller contains many
        // types of controls - such as a rudder or wheel - and they had to
        // pick some vague category that didn't really apply.
        // Here we will try some reasonable heuristics to filter out
        // badly-behaving or poorly-named keyboard/mouse devices.

        // First, check for rumblers.  It is extremely unlikely that any
        // keyboard or mouse will have a rumbler.
        Rumbler[] rumblers = controller.getRumblers();
        if (rumblers != null && rumblers.length > 0)
        {
            // If it has a rumbler let's just assume it is indeed a gamepad.
            return true;
        }

        // We'll have to do some more work now to separate the controllers
        // from the poorly-reporting hardware in the universe, as well as
        // keyboards and/or mice whose names are reported in non-English
        // form, e.g. "clavier" (keyboard in French) or "souris" (mouse in
        // French).
        Component[] components = controller.getComponents();

        // Next up, do a brute force search on the named components to see if
        // they look like those on a keyboard... let's start by getting a
        // list of all the names of the various components and converting
        // them to uppercase in the host's locale.
        Set<String> componentNames = new HashSet<String>();
        for (Component component : components)
        {
            String componentName = component.getName();
            if (componentName != null)
            {
                // Convert 
                componentNames.add(componentName.toUpperCase());
            }
        }

        // Now check if those component names are a superset of the
        // 26 Latin characters in the English alphabet / English keyboard.
        Set<String> temp = new HashSet<String>(componentNames);
        temp.retainAll(PROBABLE_ENGLISH_KEYBOARD_COMPONENT_NAMES);
        if (temp.equals(PROBABLE_ENGLISH_KEYBOARD_COMPONENT_NAMES))
        {
            // Contains all 26 letters of the english alphabet.  Almost
            // certainly a QWERTY US keyboard.
            return false;
        }

        // Failing this, weed out any non-US keyboards.  Most have function
        // keys F1-F8 at least, even if they name their other keys in
        // the default locale encoding (I suspect).  We will search on those.
        temp = new HashSet<String>(componentNames);
        temp.retainAll(PROBABLE_ANY_KEYBOARD_COMPONENT_NAMES);
        if (temp.equals(PROBABLE_ANY_KEYBOARD_COMPONENT_NAMES)
                && componentNames.size() > 50)
        {
            // Contains 8 'F' keys and at least 50 total controls (an
            // arbitrary but reasonable amount); Almost certainly a
            // keyboard of some type.
            return false;
        }

        // If we make it this far we've got something that doesn't claim
        // to be a keyboard or a mouse, doesn't have the 26 English letters
        // as component names, and doesn't have at least 50 components total
        // with 8 of them being function keys.
        // This seems a reasonable place to draw the line.
        // What could conceivably get through here would be non-English-named
        // mice that also fail to report themselves as mice.  This should be
        // (extremely) rare.
        return true;
    }

    /**
     * Load the default dead zones for the controller's components.
     * 
     * @param controller the controller to load defaults for
     * @param configuration the configuration to update
     * @param deep if <code>true</code>, descends into all subcontrollers
     * and sets the defaults for those components as well; otherwise,
     * only immediate children of the specifiec controller are processed.
     * This method skips components that aren't configured in the specified
     * controller configuration (for example, if the configuration was
     * created shallow and deep is set here to <code>true</code>, in which
     * case the controller configuration doesn't have configurations for
     * its deep children)
     */
    public final static void loadDeadZoneDefaults(
            Controller controller, ControllerConfiguration configuration,
            boolean deep)
    {
        List<Component> eligibleComponents = getComponents(controller, deep);
        for (Component component : eligibleComponents)
        {
            ComponentConfiguration componentConfig =
                configuration.getConfiguration(component);
            if (componentConfig != null)
            {
                float defaultDeadZone = component.getDeadZone();
                if (!Float.isNaN(defaultDeadZone)
                        && !Float.isInfinite(defaultDeadZone))
                {
                    // Valid float found.
                    // We only care about the absolute value; kill negatives.
                    defaultDeadZone = Math.abs(defaultDeadZone);
                    // Sanity check, clamp to range [0,1]
                    defaultDeadZone = Math.min(defaultDeadZone, 1.0f);
                    // Configure
                    componentConfig.setDeadZoneBounds(
                        -1f * defaultDeadZone, defaultDeadZone);
                }
                else
                {
                    componentConfig.setDeadZoneBounds(
                            Float.NaN, Float.NaN);
                }
            }
        }
    }

    /**
     * Load the specified dead zones for the controller's components.
     * <p>
     * This is useful for hyper-sensitive controllers that don't report
     * reasonable dead zones.  A small range such as
     * <code>[-0.05f, 0.05f]</code> (that is, 5% of the total range)
     * is usually a good choice, as it will generally compensate for random
     * jitter without making the device feel unresponsive.  Different
     * controllers may vary significantly in this regard, however, so some
     * experimentation may be necessary.
     * <p>
     * Note that the bounds are constrained by the requirements set forth
     * in {@link ComponentConfiguration#setDeadZoneBounds(float, float)}.
     * 
     * @param controller the controller to load dead zones for
     * @param configuration the configuration to update
     * @param deep if <code>true</code>, descends into all subcontrollers
     * and sets the defaults for those components as well; otherwise,
     * only immediate children of the specifiec controller are processed.
     * This method skips components that aren't configured in the specified
     * controller configuration (for example, if the configuration was
     * created shallow and deep is set here to <code>true</code>, in which
     * case the controller configuration doesn't have configurations for
     * its deep children)
     */
    public final static void loadGlobalDeadZones(
            Controller controller, ControllerConfiguration configuration,
            boolean deep, float lowerBound, float upperBound)
    {
        List<Component> eligibleComponents = getComponents(controller, deep);
        for (Component component : eligibleComponents)
        {
            ComponentConfiguration componentConfig =
                configuration.getConfiguration(component);
            if (componentConfig != null)
            {
                componentConfig.setDeadZoneBounds(
                        lowerBound, upperBound);
            }
        }
    }
}