/**
 *
 */
package org.theseed.proteins;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
import org.theseed.magic.MagicMap;

/**
 * This class tracks a set of roles.  It provides facilities for converting from role names to IDs, conflating
 * synonomous role names, and generating unique readable role IDs.  Most of its functionality is in the base
 * magic-map class.
 *
 * @author Bruce Parrello
 *
 */
public class RoleMap extends MagicMap<Role> {

    /**
     * Create a new, empty role map.
     */
    public RoleMap() {
        super(new Role());
    }

    /**
     * Find the named role.  If it does not exist, a new role object will be created.
     *
     * @param roleDesc	the role name
     *
     * @return	a Role object for the role
     */
    public Role findOrInsert(String roleDesc) {
        Role retVal = this.getByName(roleDesc);
        if (retVal == null) {
            // Create a role without an ID.
            retVal = new Role(null, roleDesc);
            // Store it in the map to create the ID.
            this.put(retVal);
        }
        return retVal;
    }

    /**
     * Save a role map to a file.  The file is 3 columns for compatability with the old system.
     *
     * @param saveFile	file to contain a text representation of the role map
     */
    public void save(File saveFile) {
        try {
            PrintWriter printer = new PrintWriter(saveFile);
            for (Role role : this.objectValues()) {
                printer.format("%s\tx\t%s%n", role.getId(), role.getName());
            }
            printer.close();
        } catch (IOException e) {
            throw new RuntimeException("Error saving role map.", e);
        }
    }

    /**
     * @return the role map saved to the specified 3-column file
     *
     * @param loadFile	the role map file to load
     */
    public static RoleMap load(File loadFile) {
        RoleMap retVal = new RoleMap();
        try (Scanner reader = new Scanner(loadFile)) {
            while (reader.hasNext()) {
                String myLine = reader.nextLine();
                String[] fields = StringUtils.splitByWholeSeparator(myLine, "\t", 3);
                retVal.addRole(fields[0], fields[2]);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading role map.", e);
        }
        return retVal;
    }

    /**
     * Add one or more roles to the map by name.
     *
     * @param roleNames		one or more role names for the role to add
     */
    public void register(String... roleNames) {
        for (String roleName : roleNames) {
            this.findOrInsert(roleName);
        }
    }

    /**
     * Add a role with a known ID to the map.
     *
     * @param roleId	ID of the role
     * @param roleDesc	name of the role
     */
    public void addRole(String roleId, String roleDesc) {
        Role newRole = new Role(roleId, roleDesc);
        this.put(newRole);
    }

}
