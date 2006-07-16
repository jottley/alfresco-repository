/*
 * Copyright (C) 2006 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */

package org.alfresco.repo.avm;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.alfresco.repo.avm.util.BulkLoader;

/**
 * Big test of AVM behavior.
 * @author britt
 */
public class AVMServiceTest extends AVMServiceTestBase
{
    /**
     * Another test of renaming in a layer.
     */
    public void testRenameLayer2()
    {
        try
        {
            TreeMap<Integer, String> history = new TreeMap<Integer, String>();
            // Set up a basic hierarchy.
            fService.createDirectory("main:/", "a");
            fService.createDirectory("main:/a", "b");
            fService.createDirectory("main:/a", "c");
            fService.createFile("main:/a/b", "foo", new ByteArrayInputStream("I am foo.".getBytes()));
            fService.createFile("main:/a/c", "bar", new ByteArrayInputStream("I am bar.".getBytes()));
            fService.createSnapshot("main");
            // History is unchanged.
            checkHistory(history, "main");
            // Make a layer to a.
            fService.createLayeredDirectory("main:/a", "main:/", "layer");
            fService.createSnapshot("main");
            // History is unchanged.
            checkHistory(history, "main");
            // /a and /layer should have identical contents.
            assertEquals(recursiveContents("main:/a", -1, true), recursiveContents("main:/layer", -1, true));
            // Now rename /layer/c/bar to /layer/b/bar
            fService.rename("main:/layer/c", "bar", "main:/layer/b", "bar");
            fService.createSnapshot("main");
            // History is unchanged.
            checkHistory(history, "main");
            // /layer/c should be empty.
            Map<String, AVMNodeDescriptor> listing = fService.getDirectoryListing(-1, "main:/layer/c");
            assertEquals(0, listing.size());
            // /layer/b should contain fao and bar
            listing = fService.getDirectoryListing(-1, "main:/layer/b");
            assertEquals(2, listing.size());
            List<String> list = new ArrayList<String>(listing.keySet());
            assertEquals("bar", list.get(0));
            assertEquals("foo", list.get(1));
            // /a/b should contain foo.
            listing = fService.getDirectoryListing(-1, "main:/a/b");
            assertEquals(1, listing.size());
            list = new ArrayList<String>(listing.keySet());
            assertEquals("foo", list.get(0));
            // /a/c should contain bar.
            listing = fService.getDirectoryListing(-1, "main:/a/c");
            assertEquals(1, listing.size());
            list = new ArrayList<String>(listing.keySet());
            assertEquals("bar", list.get(0));
            // Now make a file in /a/b
            fService.createFile("main:/a/b", "baz").close();
            fService.createSnapshot("main");
            // History is unchanged.
            checkHistory(history, "main");
            // /a/b should contain baz and foo.
            listing = fService.getDirectoryListing(-1, "main:/a/b");
            assertEquals(2, listing.size());
            list = new ArrayList<String>(listing.keySet());
            assertEquals("baz", list.get(0));
            assertEquals("foo", list.get(1));
            // /layer/b should contain foo, bar, and baz.
            listing = fService.getDirectoryListing(-1, "main:/layer/b");
            System.out.println(recursiveList("main", -1, true));
            assertEquals(3, listing.size());
            list = new ArrayList<String>(listing.keySet());
            assertEquals("bar", list.get(0));
            assertEquals("baz", list.get(1));
            assertEquals("foo", list.get(2));
            // Remove baz from /layer/b
            fService.removeNode("main:/layer/b", "baz");
            fService.createSnapshot("main");
            // History is unchanged.
            checkHistory(history, "main");
            System.out.println(recursiveList("main", -1, true));
            // /layer/b should have bar and foo.
            listing = fService.getDirectoryListing(-1, "main:/layer/b");
            assertEquals(2, listing.size());
            list = new ArrayList<String>(listing.keySet());
            assertEquals("bar", list.get(0));
            assertEquals("foo", list.get(1));
            // /a/b should contain baz and foo as before.
            listing = fService.getDirectoryListing(-1, "main:/a/b");
            assertEquals(2, listing.size());
            list = new ArrayList<String>(listing.keySet());
            assertEquals("baz", list.get(0));
            assertEquals("foo", list.get(1));
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }

    /**
     * Yet another test around rename in layers.
     */
    public void testRenameLayer3()
    {
        try
        {
            TreeMap<Integer, String> history = new TreeMap<Integer, String>();
            // Set up a handy hierarchy.
            fService.createDirectory("main:/", "a");
            fService.createDirectory("main:/a", "b");
            fService.createFile("main:/a/b", "foo").close();
            fService.createFile("main:/a/b", "bar").close();
            fService.createDirectory("main:/", "c");
            fService.createDirectory("main:/c", "d");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Make a layer over /a
            fService.createLayeredDirectory("main:/a", "main:/", "layer");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Move /c/d to /layer
            fService.rename("main:/c", "d", "main:/layer", "d");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Now make a file in /layer/d
            fService.createFile("main:/layer/d", "baz").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Make /a/d/figs and see the wackiness.
            fService.createDirectory("main:/a", "d");
            fService.createFile("main:/a/d", "figs").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /layer/d should no contain baz and figs.
            Map<String, AVMNodeDescriptor> listing = fService.getDirectoryListing(-1, "main:/layer/d");
            assertEquals(2, listing.size());
            List<String> list = new ArrayList<String>(listing.keySet());
            assertEquals("baz", list.get(0));
            assertEquals("figs", list.get(1));
            for (String val : history.values())
            {
                System.out.println(val);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Test the uncover operation.
     */
    public void testUncover()
    {
        try
        {
            TreeMap<Integer, String> history = new TreeMap<Integer, String>();
            // Set up a handy hierarchy.
            fService.createDirectory("main:/", "a");
            fService.createDirectory("main:/a/", "b");
            fService.createFile("main:/a/b", "foo").close();
            fService.createFile("main:/a/b", "bar").close();
            fService.createDirectory("main:/", "c");
            fService.createDirectory("main:/c", "d");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Make a layer over /a
            fService.createLayeredDirectory("main:/a", "main:/", "layer");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Move /c/d to /layer
            fService.rename("main:/c", "d", "main:/layer", "d");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Make a file in /layer/d
            fService.createFile("main:/layer/d", "baz").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Make /a/d/figs and see the wackiness.
            fService.createDirectory("main:/a", "d");
            fService.createFile("main:/a/d", "figs").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /layer/d should now contain baz and figs.
            Map<String, AVMNodeDescriptor> listing = fService.getDirectoryListing(-1, "main:/layer/d");
            assertEquals(2, listing.size());
            List<String> list = new ArrayList<String>(listing.keySet());
            assertEquals("baz", list.get(0));
            assertEquals("figs", list.get(1));
            // Rename /layer/d to /layer/e and uncover /layer/d
            fService.rename("main:/layer", "d", "main:/layer", "e");
            fService.uncover("main:/layer", "d");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /layer/d contains figs.
            listing = fService.getDirectoryListing(-1, "main:/layer/d");
            assertEquals(1, listing.size());
            list = new ArrayList<String>(listing.keySet());
            assertEquals("figs", list.get(0));
            // /layer/e contains baz.
            listing = fService.getDirectoryListing(-1, "main:/layer/e");
            assertEquals(1, listing.size());
            list = new ArrayList<String>(listing.keySet());
            assertEquals("baz", list.get(0));
            for (String val : history.values())
            {
                System.out.println(val);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Another test of renaming in a layer.
     */
    public void testRenameLayer4()
    {
        try
        {
            TreeMap<Integer, String> history = new TreeMap<Integer, String>();
            // Set up a handy hierarchy.
            fService.createDirectory("main:/", "a");
            fService.createDirectory("main:/a", "b");
            fService.createFile("main:/a/b", "foo").close();
            fService.createFile("main:/a/b", "bar").close();
            fService.createDirectory("main:/", "c");
            fService.createDirectory("main:/c", "d");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Make a layer over /a
            fService.createLayeredDirectory("main:/a", "main:/", "layer");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Move /layer/b to /b
            fService.rename("main:/layer", "b", "main:/", "b");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Add something to /a/b and it should show up in /b.
            fService.createFile("main:/a/b", "baz").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /b should have foo and bar and baz.
            Map<String, AVMNodeDescriptor> listing = fService.getDirectoryListing(-1, "main:/b");
            assertEquals(3, listing.size());
            List<String> list = new ArrayList<String>(listing.keySet());
            assertEquals("bar", list.get(0));
            assertEquals("baz", list.get(1));
            assertEquals("foo", list.get(2));
            // Add something to /a and it will show up in /layer.
            fService.createFile("main:/a", "figs").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /layer should have figs in it.
            listing = fService.getDirectoryListing(-1, "main:/layer");
            assertEquals(1, listing.size());
            list = new ArrayList<String>(listing.keySet());
            assertEquals("figs", list.get(0));
            for (String val : history.values())
            {
                System.out.println(val);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Test branching within branches.
     */
    public void testBranchesInBranches()
    {
        try
        {
            TreeMap<Integer, String> history = new TreeMap<Integer, String>();
            // Set up a hierarchy.
            setupBasicTree();
            // History unchanged.
            checkHistory(history, "main");
            // Make a branch from /a
            fService.createBranch(-1, "main:/a", "main:/", "abranch");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Make a branch in something that has been branched.
            fService.createBranch(-1, "main:/a/b", "main:/a", "bbranch");
            fService.createSnapshot("main");
            // History unchanged
            checkHistory(history, "main");
            // Everything under /abranch should be identical in this version 
            // and the previous.
            int version = fService.getLatestVersionID("main");
            assertEquals(recursiveContents("main:/abranch", version - 1, true),
                         recursiveContents("main:/abranch", version - 2, true));
            // Make a branch within a branch.
            fService.createBranch(-1, "main:/abranch/b/c", "main:/abranch/b", "cbranch");
            fService.createSnapshot("main");
            // History unchanged
            checkHistory(history, "main");
            // Everything under /a should be unchanged between this version and the last.
            version = fService.getLatestVersionID("main");
            assertEquals(recursiveContents("main:/a", version - 1, true),
                         recursiveContents("main:/a", version - 2, true));
            // Make a branch to something outside of a branch inside a branch.
            fService.createBranch(-1, "main:/d", "main:/abranch", "dbranch");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Make something ind /abranch/dbranch.
            fService.createFile("main:/abranch/dbranch/e/f", "baz").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // d should not have changed since the previous version.
            version = fService.getLatestVersionID("main");
            assertEquals(recursiveContents("main:/d", version - 1, true),
                         recursiveContents("main:/d", version - 2, true));
            for (String val : history.values())
            {
                System.out.println(val);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }   
                        
    /**
     * Test layers inside of layers.
     */
    public void testLayersInLayers()
    {
        try
        {
            TreeMap<Integer, String> history = new TreeMap<Integer, String>();
            setupBasicTree();
            // History unchanged.
            checkHistory(history, "main");
            // Create a layer to /a
            fService.createLayeredDirectory("main:/a", "main:/", "layer");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Make a layer inside of a layer pointing to d.
            fService.createLayeredDirectory("main:/d", "main:/layer", "under");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Create a file in /layer/under/e/f.
            fService.createFile("main:/layer/under/e/f", "baz").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Create a file in /d/e.
            fService.createFile("main:/d/e", "bow").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /layer/under/e should contain bow and f.
            Map<String, AVMNodeDescriptor> listing = fService.getDirectoryListing(-1, "main:/layer/under/e");
            assertEquals(2, listing.size());
            List<String> list = new ArrayList<String>(listing.keySet());
            assertEquals("bow", list.get(0));
            assertEquals("f", list.get(1));
            // Put a new set of dirs in to be made into a layering under d.
            fService.createDirectory("main:/", "g");
            fService.createDirectory("main:/g", "h");
            fService.createDirectory("main:/g/h", "i");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Make a layer in /d to /g.
            fService.createLayeredDirectory("main:/g", "main:/d", "gover");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /d/gover should be identical to /layer/under/gover
            assertEquals(recursiveContents("main:/d/gover", -1, true),
                         recursiveContents("main:/layer/under/gover", -1, true));
            // Create a file in /layer/under/gover/h/i
            fService.createFile("main:/layer/under/gover/h/i", "moo").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /d should be unchanged before this version and the last
            // and /g should be unchanged between this version and the last.
            int version = fService.getLatestVersionID("main");
            assertEquals(recursiveContents("main:/d", version - 1, true),
                         recursiveContents("main:/d", version - 2, true));
            assertEquals(recursiveContents("main:/g", version - 1, true),
                         recursiveContents("main:/g", version - 2, true));
            // Add a file through /d/gover/h/i
            fService.createFile("main:/d/gover/h/i", "cow").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /g should not have changed since its last version.
            version = fService.getLatestVersionID("main");
            assertEquals(recursiveContents("main:/g", version - 1, true),
                         recursiveContents("main:/g", version - 2, true));
            // /layer/under/gover/h/i shows both moo and cow.
            listing = fService.getDirectoryListing(-1, "main:/layer/under/gover/h/i");
            assertEquals(2, listing.size());
            list = new ArrayList<String>(listing.keySet());
            assertEquals("cow", list.get(0));
            assertEquals("moo", list.get(1));
            // Rename /layer/under/gover to /layer/b/gover and see what happens.
            fService.rename("main:/layer/under", "gover", "main:/layer/b", "gover");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // moo  should be in /layer/b/gover/h/i
            listing = fService.getDirectoryListing(-1, "main:/layer/b/gover/h/i");
            assertEquals(1, listing.size());
            list = new ArrayList<String>(listing.keySet());
            assertEquals("moo", list.get(0));
            // Add a new file to /layer/b/gover/h/i
            fService.createFile("main:/layer/b/gover/h/i", "oink").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /layer/b/gover/h/i should contain moo, oink.
            listing = fService.getDirectoryListing(-1, "main:/layer/b/gover/h/i");
            assertEquals(2, listing.size());
            list = new ArrayList<String>(listing.keySet());
            assertEquals("moo", list.get(0));
            assertEquals("oink", list.get(1));
            for (String val : history.values())
            {
                System.out.println(val);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Test behavior when one branches a layer.
     */
    public void testLayerAndBranch()
    {
        try
        {
            TreeMap<Integer, String> history = new TreeMap<Integer, String>();
            // Create a basic tree.
            fService.createDirectory("main:/", "a");
            fService.createDirectory("main:/a", "b");
            fService.createDirectory("main:/a/b", "c");
            fService.createFile("main:/a/b/c", "foo").close();
            fService.createFile("main:/a/b/c", "bar").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Create a layer over /a
            fService.createLayeredDirectory("main:/a", "main:/", "layer");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /a and /layer should have identical contents.
            assertEquals(recursiveContents("main:/a", -1, true),
                         recursiveContents("main:/layer", -1, true));
            // Make a modification in /layer
            fService.createFile("main:/layer/b", "baz").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Branch off layer.
            fService.createBranch(-1, "main:/layer", "main:/", "branch");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /layer/b and /branch/b should have identical contents.
            assertEquals(recursiveContents("main:/layer/b", -1, true),
                         recursiveContents("main:/branch/b", -1, true));
            // Create /branch/b/c/foo
            fService.createFile("main:/branch/b/c", "baz").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /layer should not have changed.
            int version = fService.getLatestVersionID("main");
            assertEquals(recursiveContents("main:/layer", version - 1, true),
                         recursiveContents("main:/layer", version - 2, true));
            // Change something in /layer
            fService.createFile("main:/layer/b/c", "fig").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /branch should not have changed.
            version = fService.getLatestVersionID("main");
            assertEquals(recursiveContents("main:/branch", version - 1, true),
                         recursiveContents("main:/branch", version - 2, true));
            // Create another layer on /a
            fService.createLayeredDirectory("main:/a", "main:/", "layer2");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Branch from /layer2/b.
            fService.createBranch(-1, "main:/layer2/b", "main:/", "branch2");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Create something in the branch.
            fService.createFile("main:/branch2", "goofy").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /layer2 should be unchanged.
            version = fService.getLatestVersionID("main");
            assertEquals(recursiveContents("main:/layer2", version - 1, true),
                         recursiveContents("main:/layer2", version - 2, true));
            // Remove something from /layer2
            fService.removeNode("main:/layer2/b/c", "foo");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /branch2 is unchanged.
            version = fService.getLatestVersionID("main");
            assertEquals(recursiveContents("main:/branch2", version - 1, true),
                         recursiveContents("main:/branch2", version - 2, true));
            // /a is unchanged.
            assertEquals(recursiveContents("main:/a", version - 1, true),
                         recursiveContents("main:/a", version - 2, true));
            for (String val : history.values())
            {
                System.out.println(val);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }   
       
    /**
     * Test scenario in which something is renamed from inside one independent layer to another.
     */
    public void testRenameLayerToLayer()
    {
        try
        {
            TreeMap<Integer, String> history = new TreeMap<Integer, String>();
            // Set up two trees
            fService.createDirectory("main:/", "a");
            fService.createDirectory("main:/a", "b");
            fService.createDirectory("main:/a/b", "c");
            fService.createFile("main:/a/b/c", "foo").close();
            fService.createFile("main:/a/b/c", "bar").close();
            fService.createDirectory("main:/", "d");
            fService.createDirectory("main:/d", "e");
            fService.createDirectory("main:/d/e", "f");
            fService.createFile("main:/d/e/f", "moo").close();
            fService.createFile("main:/d/e/f", "cow").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Make a layer to /a and a layer to /d
            fService.createLayeredDirectory("main:/a", "main:/", "la");
            fService.createLayeredDirectory("main:/d", "main:/", "ld");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Move /la/b/c to /ld/e/c.
            fService.rename("main:/la/b", "c", "main:/ld/e", "c");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Create file /ld/e/c/baz.
            fService.createFile("main:/ld/e/c", "baz").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Here's the thing we'd like to assert.
            assertEquals("main:/a/b/c", fService.lookup(-1, "main:/ld/e/c").getIndirection());
            for (String val : history.values())
            {
                System.out.println(val);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
        
    /**
     * Test Nothing.  Just make sure set up works.
     */
    public void testNothing()
    {
    }
    
    /**
     * Test making a simple directory.
     */
    public void testCreateDirectory()
    {
        try
        {
            fService.createDirectory("main:/", "testdir");
            fService.createSnapshot("main");
            assertEquals(AVMNodeType.PLAIN_DIRECTORY, fService.lookup(-1, "main:/").getType());
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Test creating a file.
     */
    public void testCreateFile()
    {
        try
        {
            testCreateDirectory();
            fService.createFile("main:/testdir", "testfile").close();
            fService.createFile("main:/", "testfile2").close();
            fService.createSnapshot("main");
            PrintStream out = new PrintStream(fService.getFileOutputStream("main:/testdir/testfile"));
            out.println("This is testdir/testfile");
            out.close();
            out = new PrintStream(fService.getFileOutputStream("main:/testfile2"));
            out.println("This is testfile2");
            out.close();
            fService.createSnapshot("main");
            List<VersionDescriptor> versions = fService.getAVMStoreVersions("main");
            for (VersionDescriptor version : versions)
            {
                System.out.println("V:" + version.getVersionID());
                System.out.println(recursiveList("main", version.getVersionID(), true));
            }
            BufferedReader reader = 
                new BufferedReader(new InputStreamReader(fService.getFileInputStream(-1, "main:/testdir/testfile")));
            String line = reader.readLine();
            assertEquals("This is testdir/testfile", line);
            reader.close();
            reader =
                new BufferedReader(new InputStreamReader(fService.getFileInputStream(-1, "main:/testfile2")));
            line = reader.readLine();
            assertEquals("This is testfile2", line);
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Test creating a branch.
     */
    public void testCreateBranch()
    {
        try
        {
            setupBasicTree();
            fService.createBranch(-1, "main:/a", "main:/d/e", "abranch");
            fService.createSnapshot("main");
            List<VersionDescriptor> versions = fService.getAVMStoreVersions("main");
            for (VersionDescriptor version : versions)
            {
                System.out.println("V:" + version.getVersionID());
                System.out.println(recursiveList("main", version.getVersionID(), true));
            }
            String original = recursiveList("main:/a", -1, 0, true);
            original = original.substring(original.indexOf('\n'));
            String branch = recursiveList("main:/d/e/abranch", -1, 0, true);
            branch = branch.substring(branch.indexOf('\n'));
            assertEquals(original, branch);
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Test creating a layer.
     */
    public void testCreateLayer()
    {
        try
        {
            setupBasicTree();
            fService.createLayeredDirectory("main:/a", "main:/d/e", "alayer");
            fService.createSnapshot("main");
            System.out.println(recursiveList("main", -1, true));
            assertEquals("main:/a", fService.getIndirectionPath(-1, "main:/d/e/alayer"));
            assertEquals(recursiveContents("main:/a", -1, true),
                         recursiveContents("main:/d/e/alayer", -1, true));
            PrintStream out = new PrintStream(fService.getFileOutputStream("main:/d/e/alayer/b/c/foo"));
            out.println("I am main:/d/e/alayer/b/c/foo");
            out.close();
            fService.createSnapshot("main");
            BufferedReader reader = 
                new BufferedReader(new InputStreamReader(fService.getFileInputStream(-1, "main:/a/b/c/foo")));
            String line = reader.readLine();
            reader.close();
            assertEquals("I am main:/a/b/c/foo", line);
            System.out.println(recursiveList("main", -1, true));
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Test creating a layered file.
     */
    public void testCreateLayeredFile()
    {
        try
        {
            setupBasicTree();
            fService.createLayeredFile("main:/a/b/c/foo", "main:/d", "lfoo");
            fService.createSnapshot("main");
            System.out.println(recursiveList("main", -1, true));
            assertEquals("main:/a/b/c/foo", fService.getIndirectionPath(-1, "main:/d/lfoo"));
            BufferedReader reader = 
                new BufferedReader(new InputStreamReader(fService.getFileInputStream(-1, "main:/d/lfoo")));
            String line = reader.readLine();
            reader.close();
            assertEquals("I am main:/a/b/c/foo", line);
            PrintStream out = new PrintStream(fService.getFileOutputStream("main:/d/lfoo"));
            out.println("I am main:/d/lfoo");
            out.close();
            fService.createSnapshot("main");
            System.out.println(recursiveList("main", -1, true));
            reader = 
                new BufferedReader(new InputStreamReader(fService.getFileInputStream(-1, "main:/a/b/c/foo")));
            line = reader.readLine();
            reader.close();
            assertEquals("I am main:/a/b/c/foo", line);
            reader =
                new BufferedReader(new InputStreamReader(fService.getFileInputStream(-1, "main:/d/lfoo")));
            line = reader.readLine();
            reader.close();
            assertEquals("I am main:/d/lfoo", line);
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Test rename.
     */
    public void testRename()
    {
        try
        {
            setupBasicTree();
            fService.rename("main:/a", "b", "main:/d/e", "brenamed");
            fService.createSnapshot("main");
            System.out.println(recursiveList("main", -1, true));
            assertEquals(recursiveContents("main:/a/b", 1, true),
                         recursiveContents("main:/d/e/brenamed", 2, true));
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Test remove.
     */
    public void testRemove()
    {
        try
        {
            setupBasicTree();
            TreeMap<Integer, String> history = new TreeMap<Integer, String>();
            checkHistory(history, "main");
            System.out.println(history.get(0));
            fService.removeNode("main:/a/b/c", "foo");
            fService.createSnapshot("main");
            checkHistory(history, "main");
            System.out.println(history.get(1));
            Map<String, AVMNodeDescriptor> l = fService.getDirectoryListing(-1, "main:/a/b/c");
            assertEquals(1, l.size());
            fService.removeNode("main:/d", "e");
            fService.createSnapshot("main");
            checkHistory(history, "main");
            System.out.println(history.get(2));
            l = fService.getDirectoryListing(-1, "main:/d");
            assertEquals(0, l.size());
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
   
    /**
     * Test branching from one AVMStore to another.
     */
    public void testBranchAcross()
    {
        try
        {
            setupBasicTree();
            fService.createAVMStore("second");
            List<AVMStoreDescriptor> repos = fService.getAVMStores();
            assertEquals(2, repos.size());
            System.out.println(repos.get(0));
            System.out.println(repos.get(1));
            fService.createBranch(-1, "main:/", "second:/", "main");
            fService.createSnapshot("second");
            System.out.println(recursiveList("second", -1, true));
            assertEquals(recursiveContents("main:/", -1, true),
                         recursiveContents("second:/main", -1, true));
            // Now make sure nothing happens to the branched from place,
            // if the branch is modified.
            PrintStream out = 
                new PrintStream(fService.getFileOutputStream("second:/main/a/b/c/foo"));
            out.println("I am second:/main/a/b/c/foo");
            out.close();
            fService.createSnapshot("second");
            System.out.println(recursiveList("second", -1, true));
            BufferedReader reader = 
                new BufferedReader(new InputStreamReader(fService.getFileInputStream(-1, "main:/a/b/c/foo")));
            String line = reader.readLine();
            reader.close();
            assertEquals("I am main:/a/b/c/foo", line);
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Test creating a layer across AVMStores.
     */
    public void testLayerAcross()
    {
        try
        {
            setupBasicTree();
            fService.createAVMStore("second");
            fService.createLayeredDirectory("main:/", "second:/", "main");
            fService.createSnapshot("second");
            System.out.println(recursiveList("second", -1, true));
            assertEquals(recursiveContents("main:/", -1, true),
                         recursiveContents("second:/main", -1, true));
            // Now make sure that a copy on write will occur and
            // that the underlying stuff doesn't get changed.
            PrintStream out = new PrintStream(fService.getFileOutputStream("second:/main/a/b/c/foo"));
            out.println("I am second:/main/a/b/c/foo");
            out.close();
            fService.createSnapshot("second");
            System.out.println(recursiveList("second", -1, true));
            BufferedReader reader = 
                new BufferedReader(new InputStreamReader(fService.getFileInputStream(-1, "second:/main/a/b/c/foo")));
            String line = reader.readLine();
            reader.close();
            assertEquals("I am second:/main/a/b/c/foo", line);
            reader =
                new BufferedReader(new InputStreamReader(fService.getFileInputStream(-1, "main:/a/b/c/foo")));
            line = reader.readLine();
            reader.close();
            assertEquals("I am main:/a/b/c/foo", line);
            fService.purgeAVMStore("second");
            fService.purgeVersion(1, "main");
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();            
        }
    }
    
    /**
     * Test rename across AVMStores.
     */
    public void testRenameAcross()
    {
        try
        {
            setupBasicTree();
            fService.createAVMStore("second");
            fService.rename("main:/a/b", "c", "second:/", "cmoved");
            ArrayList<String> toSnapshot = new ArrayList<String>();
            toSnapshot.add("main");
            toSnapshot.add("second");
            System.out.println(recursiveList("main", -1, true));
            System.out.println(recursiveList("second", -1, true));
            // Check that the moved thing has identical contents to the thing it
            // was moved from.
            assertEquals(recursiveContents("main:/a/b/c", 1, true),
                         recursiveContents("second:/cmoved", -1, true));
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Test COW in various circumstances.
     */
    public void testDeepCOW()
    {
        try
        {
            // Makes a layer on top of a layer on top of a plain directory.
            // Assures that the correct layers are copied when files
            // are added in the two layers.
            fService.createDirectory("main:/", "a");
            fService.createDirectory("main:/a", "b");
            fService.createSnapshot("main");
            Map<String, AVMNodeDescriptor> listing = fService.getDirectoryListing(-1, "main:/a");
            assertEquals(1, listing.size());
            List<String> list = new ArrayList<String>(listing.keySet());
            assertEquals("b", list.get(0));
            fService.createLayeredDirectory("main:/a", "main:/", "c");
            fService.createLayeredDirectory("main:/c", "main:/", "d");
            fService.createFile("main:/d/b", "foo.txt").close();
            fService.createSnapshot("main");
            System.out.println(recursiveList("main", -1, true));
            listing = fService.getDirectoryListing(-1, "main:/d/b");
            assertEquals(1, listing.size());
            list = new ArrayList<String>(listing.keySet());
            assertEquals("foo.txt", list.get(0));
            fService.createFile("main:/c/b", "bar.txt").close();
            fService.createSnapshot("main");
            System.out.println(recursiveList("main", -1, true));
            listing = fService.getDirectoryListing(-1, "main:/c/b");
            assertEquals(1, listing.size());
            list = new ArrayList<String>(listing.keySet());
            assertEquals("bar.txt", list.get(0));
            listing = fService.getDirectoryListing(-1, "main:/d/b");
            assertEquals(2, listing.size());
            list = new ArrayList<String>(listing.keySet());
            assertEquals("bar.txt", list.get(0));
            assertEquals("foo.txt", list.get(1));
            fService.rename("main:/", "c", "main:/", "e");
            fService.createSnapshot("main");
            System.out.println(recursiveList("main", -1, true));
            listing = fService.getDirectoryListing(-1, "main:/d/b");
            assertEquals(1, listing.size());
            list = new ArrayList<String>(listing.keySet());
            assertEquals("foo.txt", list.get(0));
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Test branching and layering interaction.
     */
    public void testBranchAndLayer()
    {
        try
        {
            // Create a simple directory hierarchy.
            fService.createDirectory("main:/", "a");
            fService.createDirectory("main:/a", "b");
            fService.createFile("main:/a/b", "c.txt").close();
            fService.createFile("main:/a/b", "d.txt").close();
            fService.createFile("main:/a", "e.txt").close();
            fService.createSnapshot("main");
            // Make a branch off of a.
            fService.createBranch(-1, "main:/a", "main:/", "branch");
            fService.createSnapshot("main");
            // The branch should contain exactly the same things as the thing
            // it branched from.
            assertEquals(recursiveContents("main:/a", -1, true),
                         recursiveContents("main:/branch", -1, true));
            // Make a layer pointing to /branch/b
            fService.createLayeredDirectory("main:/branch/b", "main:/", "layer");
            fService.createSnapshot("main");
            // The new layer should contain exactly the same things as the thing it is layered to.
            assertEquals(recursiveContents("main:/branch/b", -1, true),
                         recursiveContents("main:/layer", -1, true));
            // Make a modification in /a/b, the original branch.
            PrintStream out = new PrintStream(fService.getFileOutputStream("main:/a/b/c.txt"));
            out.println("I am c, modified in main:/a/b.");
            out.close();
            fService.createSnapshot("main");
            // The layer should still have identical content to /branch/b.
            assertEquals(recursiveContents("main:/branch/b", -1, true),
                         recursiveContents("main:/layer", -1, true));
            // But the layer won't have contents identical to /a/b's
            assertFalse(recursiveContents("main:/a/b", -1, true).equals(recursiveContents("main:/layer", -1, true)));
            // Make a modification in /branch/b
            out = new PrintStream(fService.getFileOutputStream("main:/branch/b/d.txt"));
            out.println("I am d, modified in main:/branch/b");
            out.close();
            fService.createSnapshot("main");
            // The layer contents should be identical to the latest contents of /branch/b.
            assertEquals(recursiveContents("main:/branch/b", -1, true),
                         recursiveContents("main:/layer", -1, true));
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Test basic Layering.
     */
    public void testLayering()
    {
        try
        {
            // Make some directories;
            fService.createDirectory("main:/", "a");
            fService.createDirectory("main:/a", "b");
            fService.createDirectory("main:/a/b", "c");
            fService.createDirectory("main:/a/b/c", "d");
            fService.createSnapshot("main");
            // Now make some layers.  Three to be precise.
            fService.createLayeredDirectory("main:/a", "main:/", "e");
            fService.createLayeredDirectory("main:/e", "main:/", "f");
            fService.createLayeredDirectory("main:/f", "main:/", "g");
            fService.createSnapshot("main");
            // e, f, g should all have the same contents as a.
            String a = recursiveContents("main:/a", -1, true);
            String e = recursiveContents("main:/e", -1, true);
            String f = recursiveContents("main:/f", -1, true);
            String g = recursiveContents("main:/g", -1, true);
            assertEquals(a, e);
            assertEquals(a, f);
            assertEquals(a, g);
            // Now make a file in /g/b/c/d and /f/b/c/d
            fService.createFile("main:/g/b/c/d", "foo").close();
            fService.createFile("main:/f/b/c/d", "bar").close();
            fService.createSnapshot("main");
            // /g/b/c/d should contain foo and bar.
            Map<String, AVMNodeDescriptor> listing = fService.getDirectoryListing(-1, "main:/g/b/c/d");
            assertEquals(2, listing.size());
            List<String> list = new ArrayList<String>(listing.keySet());
            assertEquals("bar", list.get(0));
            assertEquals("foo", list.get(1));
            // /f/b/c/d should contain just bar.
            listing = fService.getDirectoryListing(-1, "main:/f/b/c/d");
            assertEquals(1, listing.size());
            list = new ArrayList<String>(listing.keySet());
            assertEquals("bar", list.get(0));
            // Now do something in the bottom layer.
            fService.createFile("main:/a/b/c", "baz").close();
            fService.createSnapshot("main");
            // /e/b/c should contain baz and d
            listing = fService.getDirectoryListing(-1, "main:/e/b/c");
            assertEquals(2, listing.size());
            list = new ArrayList<String>(listing.keySet());
            assertEquals("baz", list.get(0));
            assertEquals("d", list.get(1));
            // Now add something in the e layer.
            fService.createFile("main:/e/b/c/d", "bing").close();
            fService.createSnapshot("main");
            // /f/b/c/d should now contain bar and bing.
            listing = fService.getDirectoryListing(-1, "main:/f/b/c/d");
            assertEquals(2, listing.size());
            list = new ArrayList<String>(listing.keySet());
            assertEquals("bar", list.get(0));
            assertEquals("bing", list.get(1));
            System.out.println(recursiveList("main", -1, true));
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Test rename within a layer.
     */
    public void testRenameInLayer()
    {
        try
        {
            // Setup a base hierarchy.
            fService.createDirectory("main:/", "a");
            fService.createDirectory("main:/a", "b");
            fService.createDirectory("main:/a/b", "c");
            fService.createDirectory("main:/a", "d");
            fService.createSnapshot("main");
            // Now make a layer to a.
            fService.createLayeredDirectory("main:/a", "main:/", "layer");
            fService.createSnapshot("main");
            // /layer should have the same contents as /a at this point.
            assertEquals(recursiveContents("main:/a", -1, true),
                         recursiveContents("main:/layer", -1, true));
            // Now we will rename /layer/d to /layer/moved
            fService.rename("main:/layer", "d", "main:/layer", "moved");
            fService.createSnapshot("main");
            // /layer should contain b and moved
            Map<String, AVMNodeDescriptor> listing = fService.getDirectoryListing(-1, "main:/layer");
            assertEquals(2, listing.size());
            List<String> list = new ArrayList<String>(listing.keySet());
            assertEquals("b", list.get(0));
            assertEquals("moved", list.get(1));
            // Now rename moved back to d.
            fService.rename("main:/layer", "moved", "main:/layer", "d");
            fService.createSnapshot("main");
            // /layer should contain b and d.
            listing = fService.getDirectoryListing(-1, "main:/layer");
            assertEquals(2, listing.size());
            list = new ArrayList<String>(listing.keySet());
            assertEquals("b", list.get(0));
            assertEquals("d", list.get(1));
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Test behavior of multiply layers not in register.
     */
    public void testMultiLayerUnregistered()
    {
        try
        {
            TreeMap<Integer, String> history = new TreeMap<Integer, String>();
            setupBasicTree();
            // History unchanged.
            checkHistory(history, "main");
            // Create layered directory /d/e/f/ to /a
            fService.createLayeredDirectory("main:/a", "main:/d/e/f", "l0");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Create layered directory /d/l1 to /d/e/f.
            fService.createLayeredDirectory("main:/d/e/f", "main:/d", "l1");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Create layered directory /l2 to /d
            fService.createLayeredDirectory("main:/d", "main:/", "l2");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Create /l2/l1/l0/a/foo.
            fService.createFile("main:/l2/l1/l0/b", "foo").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /l2/l1/l0 should now point at /d/l1/l0
            assertEquals("main:/d/l1/l0", fService.lookup(-1, "main:/l2/l1/l0").getIndirection());
            // /l2/l1/l0/b should now point at /d/l1/l0/b
            assertEquals("main:/d/l1/l0/b", fService.lookup(-1, "main:/l2/l1/l0/b").getIndirection());
            for (String val : history.values())
            {
                System.out.println(val);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Test makePrimary.
     */
    public void testMakePrimary()
    {
        try
        {
            TreeMap<Integer, String> history = new TreeMap<Integer, String>();
            setupBasicTree();
            // History unchanged.
            checkHistory(history, "main");
            // Make a layer to /a
            fService.createLayeredDirectory("main:/a", "main:/", "layer");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Make /layer/b/c primary.
            fService.makePrimary("main:/layer/b/c");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Rename /layer/b/c to /layer/c
            fService.rename("main:/layer/b", "c", "main:/layer", "c");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /a/b/c should have identical contents to /layer/c
            assertEquals(recursiveContents("main:/a/b/c", -1, true),
                         recursiveContents("main:/layer/c", -1, true));
            // Create /layer2 to /a.
            fService.createLayeredDirectory("main:/a", "main:/", "layer2");
            // Make a file down in /layer2/b/c
            fService.createFile("main:/layer2/b/c", "baz").close();
            // make /layer2/b/c primary.
            fService.makePrimary("main:/layer2/b/c");
            // Rename /layer2/b/c to /layer2/c
            fService.rename("main:/layer2/b", "c", "main:/layer2", "c");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /layer2/c should contain foo bar and baz.
            Map<String, AVMNodeDescriptor> listing = fService.getDirectoryListing(-1, "main:/layer2/c");
            assertEquals(3, listing.size());
            List<String> list = new ArrayList<String>(listing.keySet());
            assertEquals("bar", list.get(0));
            assertEquals("baz", list.get(1));
            assertEquals("foo", list.get(2));
            for (String val : history.values())
            {
                System.out.println(val);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
            
    /**
     * Test retargeting a directory.
     */
    public void testRetarget()
    {
        try
        {
            TreeMap<Integer, String> history = new TreeMap<Integer, String>();
            setupBasicTree();
            // History unchanged.
            checkHistory(history, "main");
            // Make a layer to /a
            fService.createLayeredDirectory("main:/a", "main:/", "layer");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Retarget /layer/b/c to /d.
            fService.retargetLayeredDirectory("main:/layer/b/c", "main:/d");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /layer/b/c should contain e.
            Map<String, AVMNodeDescriptor> listing = fService.getDirectoryListing(-1, "main:/layer/b/c");
            assertEquals(1, listing.size());
            List<String> list = new ArrayList<String>(listing.keySet());
            assertEquals("e", list.get(0));
            // Rename /layer/b/c to /layer/c
            fService.rename("main:/layer/b", "c", "main:/layer", "c");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /d should have identical contents to /layer/c
            assertEquals(recursiveContents("main:/d", -1, true),
                         recursiveContents("main:/layer/c", -1, true));
            // Create /layer2 to /a.
            fService.createLayeredDirectory("main:/a", "main:/", "layer2");
            // Make a file down in /layer2/b/c
            fService.createFile("main:/layer2/b/c", "baz").close();
            // make /layer2/b/c primary.
            fService.retargetLayeredDirectory("main:/layer2/b/c", "main:/d");
            // Rename /layer2/b/c to /layer2/c
            fService.rename("main:/layer2/b", "c", "main:/layer2", "c");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /layer2/c should have baz and e in it.
            listing = fService.getDirectoryListing(-1, "main:/layer2/c");
            assertEquals(2, listing.size());
            list = new ArrayList<String>(listing.keySet());
            assertEquals("baz", list.get(0));
            assertEquals("e", list.get(1));
            for (String val : history.values())
            {
                System.out.println(val);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Test rename between branches.
     */
    public void testRenameBranchToBranch()
    {
        try
        {
            TreeMap<Integer, String> history = new TreeMap<Integer, String>();
            // Set up two trees
            fService.createDirectory("main:/", "a");
            fService.createDirectory("main:/a", "b");
            fService.createDirectory("main:/a/b", "c");
            fService.createFile("main:/a/b/c", "foo").close();
            fService.createFile("main:/a/b/c", "bar").close();
            fService.createDirectory("main:/", "d");
            fService.createDirectory("main:/d", "e");
            fService.createDirectory("main:/d/e", "f");
            fService.createFile("main:/d/e/f", "moo").close();
            fService.createFile("main:/d/e/f", "cow").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Make branches.
            fService.createBranch(-1, "main:/a/b", "main:/", "abranch");
            fService.createBranch(-1, "main:/d/e", "main:/", "dbranch");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Move /abranch/c/foo /dbranch/foo
            fService.rename("main:/abranch/c", "foo", "main:/dbranch", "foo");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Confirm that /a and /d are unchanged.
            int version = fService.getLatestVersionID("main");
            assertEquals(recursiveContents("main:/a", version - 1, true),
                         recursiveContents("main:/a", version - 2, true));
            assertEquals(recursiveContents("main:/d", version - 1, true),
                         recursiveContents("main:/d", version - 2, true));
            // Move /dbranch/f to /abranch/c/f
            fService.rename("main:/dbranch", "f", "main:/abranch/c", "f");
            fService.createSnapshot("main");
            // Confirm that /a and /d are unchanged.
            version = fService.getLatestVersionID("main");
            assertEquals(recursiveContents("main:/a", version - 1, true),
                         recursiveContents("main:/a", version - 2, true));
            assertEquals(recursiveContents("main:/d", version - 1, true),
                         recursiveContents("main:/d", version - 2, true));
            // History unchanged.
            checkHistory(history, "main");
            for (String val : history.values())
            {
                System.out.println(val);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }

    /**
     * Test a branch being created in a layer.
     */
    public void testBranchIntoLayer()
    {
        try
        {
            TreeMap<Integer, String> history = new TreeMap<Integer, String>();
            // Set up two trees
            fService.createDirectory("main:/", "a");
            fService.createDirectory("main:/a", "b");
            fService.createDirectory("main:/a/b", "c");
            fService.createFile("main:/a/b/c", "foo").close();
            fService.createFile("main:/a/b/c", "bar").close();
            fService.createDirectory("main:/", "d");
            fService.createDirectory("main:/d", "e");
            fService.createDirectory("main:/d/e", "f");
            fService.createFile("main:/d/e/f", "moo").close();
            fService.createFile("main:/d/e/f", "cow").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Create a layer to /a
            fService.createLayeredDirectory("main:/a", "main:/", "layer");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Now create a branch from /d in /layer/a/b.
            fService.createBranch(-1, "main:/d", "main:/layer/b", "branch");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Now modify /layer/b/branch/e/f/moo.
            PrintStream out = new PrintStream(fService.getFileOutputStream("main:/layer/b/branch/e/f/moo"));
            out.println("moo modified.");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /layer/b/branch/e/f should contain moo and cow.
            Map<String, AVMNodeDescriptor> listing = fService.getDirectoryListing(-1, "main:/layer/b/branch/e/f");
            assertEquals(2, listing.size());
            List<String> list = new ArrayList<String>(listing.keySet());
            assertEquals("cow", list.get(0));
            assertEquals("moo", list.get(1));
            for (String val : history.values())
            {
                System.out.println(val);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Test renaming into a layer.
     */
    public void testRenameIntoLayer()
    {
        try
        {
            TreeMap<Integer, String> history = new TreeMap<Integer, String>();
            // Set up two trees
            fService.createDirectory("main:/", "a");
            fService.createDirectory("main:/a", "b");
            fService.createDirectory("main:/a/b", "c");
            fService.createFile("main:/a/b/c", "foo").close();
            fService.createFile("main:/a/b/c", "bar").close();
            fService.createDirectory("main:/", "d");
            fService.createDirectory("main:/d", "e");
            fService.createDirectory("main:/d/e", "f");
            fService.createFile("main:/d/e/f", "moo").close();
            fService.createFile("main:/d/e/f", "cow").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Create a layer to /a
            fService.createLayeredDirectory("main:/a", "main:/", "layer");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Now rename /d into /layer/a/b.
            fService.rename("main:/", "d", "main:/layer/b", "d");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Now modify /layer/b/branch/e/f/moo.
            PrintStream out = new PrintStream(fService.getFileOutputStream("main:/layer/b/d/e/f/moo"));
            out.println("moo modified.");
            out.close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /layer/b/branch/e/f should contain moo and cow.
            Map<String, AVMNodeDescriptor> listing = fService.getDirectoryListing(-1, "main:/layer/b/d/e/f");
            assertEquals(2, listing.size());
            List<String> list = new ArrayList<String>(listing.keySet());
            assertEquals("cow", list.get(0));
            assertEquals("moo", list.get(1));
            for (String val : history.values())
            {
                System.out.println(val);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Test proper indirection behavior.
     */
    public void testIndirectionBehavior()
    {
        try
        {
            TreeMap<Integer, String> history = new TreeMap<Integer, String>();
            // Setup the stage.
            fService.createDirectory("main:/", "a");
            fService.createDirectory("main:/a", "b");
            fService.createDirectory("main:/a/b", "c");
            fService.createDirectory("main:/a/b/c", "d");
            fService.createDirectory("main:/a/b/c/d", "e");
            fService.createLayeredDirectory("main:/a", "main:/", "layer");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            fService.createDirectory("main:/", "f");
            fService.createDirectory("main:/f", "g");
            fService.createDirectory("main:/f/g", "h");
            fService.createLayeredDirectory("main:/f", "main:/", "flayer");
            fService.createLayeredDirectory("main:/flayer", "main:/layer/b/c", "fover");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            fService.createDirectory("main:/", "i");
            fService.createDirectory("main:/i", "j");
            fService.createDirectory("main:/i/j", "k");
            fService.createLayeredDirectory("main:/i", "main:/f/g/h", "iover");
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            fService.createFile("main:/layer/b/c/fover/g/h/iover/j/k", "foo").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // Make a file in /i/j/k
            fService.createFile("main:/i/j/k", "pismo").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /layer/b/c/fover/g/h/iover/j/k should contain pismo and foo.
            Map<String, AVMNodeDescriptor> listing = fService.getDirectoryListing(-1, "main:/layer/b/c/fover/g/h/iover/j/k");
            assertEquals(2, listing.size());
            List<String> list = new ArrayList<String>(listing.keySet());
            assertEquals("foo", list.get(0));
            assertEquals("pismo", list.get(1));
            // Make a file in /flayer/g/h/iover/j/k
            fService.createFile("main:/flayer/g/h/iover/j/k", "zuma").close();
            fService.createSnapshot("main");
            // History unchanged.
            checkHistory(history, "main");
            // /layer/b/c/fover/g/h/iover/j/k should contain foo, pismo, and zuma.
            listing = fService.getDirectoryListing(-1, "main:/layer/b/c/fover/g/h/iover/j/k");
            assertEquals(3, listing.size());
            list = new ArrayList<String>(listing.keySet());
            assertEquals("foo", list.get(0));
            assertEquals("pismo", list.get(1));
            assertEquals("zuma", list.get(2));
            for (String val : history.values())
            {
                System.out.println(val);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }

    /**
     * Test reading of versioned content via a layer.
     */
    public void testVersionedRead()
    {
        try
        {
            PrintStream out = new PrintStream(fService.createFile("main:/", "foo"));
            out.print("version1");
            out.close();
            fService.createLayeredFile("main:/foo", "main:/", "afoo");
            fService.createSnapshot("main");
            assertEquals(8, fService.lookup(-1, "main:/foo").getLength());
            out = new PrintStream(fService.getFileOutputStream("main:/foo"));
            out.print("version2");
            out.close();
            fService.createSnapshot("main");
            BufferedReader reader = 
                new BufferedReader(new InputStreamReader(fService.getFileInputStream(1, "main:/afoo")));
            assertEquals("version2", reader.readLine());
            reader.close();
            reader = 
                new BufferedReader(new InputStreamReader(fService.getFileInputStream(2, "main:/afoo")));
            assertEquals("version2", reader.readLine());
            reader.close();
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }

    /**
     * Test rename of an overlayed directory contained in an overlayed
     * directory.
     */
    public void testRenameLayerInLayer()
    {
        try
        {
            // Make some directories.
            fService.createDirectory("main:/", "a");
            fService.createDirectory("main:/a", "b");
            fService.createDirectory("main:/a/b", "c");
            fService.createSnapshot("main");
            // Make a layer to /a.
            fService.createLayeredDirectory("main:/a", "main:/", "layer");
            fService.createSnapshot("main");
            // Force a copy on write in the layer.
            fService.createFile("main:/layer/b/c", "foo").close();
            fService.createSnapshot("main");
            assertEquals("main:/a/b/c", fService.lookup(-1, "main:/layer/b/c").getIndirection());
            // Now rename.
            fService.rename("main:/layer/b", "c", "main:/layer/b", "d");
            fService.createSnapshot("main");
            assertEquals("main:/a/b/d", fService.lookup(-1, "main:/layer/b/d").getIndirection());
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
 
    /**
     * Yet another rename from layer to layer test.
     */
    public void testAnotherRename()
    {
        try
        {
            // Make two directory trees.
            fService.createDirectory("main:/", "a");
            fService.createDirectory("main:/a", "b");
            fService.createDirectory("main:/a/b", "c");
            fService.createDirectory("main:/", "d");
            fService.createDirectory("main:/d", "e");
            fService.createDirectory("main:/d/e", "f");
            fService.createSnapshot("main");
            // Make a layer over each.
            fService.createLayeredDirectory("main:/a", "main:/", "la");
            fService.createLayeredDirectory("main:/d", "main:/", "ld");
            fService.createSnapshot("main");
            // rename from down in one layer to another.
            fService.rename("main:/ld/e", "f", "main:/la/b", "f");
            fService.createSnapshot("main");
            AVMNodeDescriptor desc = fService.lookup(-1, "main:/la/b/f");
            assertTrue(desc.isPrimary());
            assertEquals("main:/d/e/f", desc.getIndirection());
            // Now rename in in the layer.
            fService.rename("main:/la/b", "f", "main:/la/b/c", "f");
            fService.createSnapshot("main");
            desc = fService.lookup(-1, "main:/la/b/c/f");
            assertTrue(desc.isPrimary());
            assertEquals("main:/d/e/f", desc.getIndirection());  
            // Now create a directory in the layered f.
            fService.createDirectory("main:/la/b/c/f", "dir");
            fService.createSnapshot("main");
            desc = fService.lookup(-1, "main:/la/b/c/f/dir");
            assertFalse(desc.isPrimary());
            assertEquals("main:/d/e/f/dir", desc.getIndirection());
            // Now rename that and see where it points.
            fService.rename("main:/la/b/c/f", "dir", "main:/la/b", "dir");
            fService.createSnapshot("main");
            desc = fService.lookup(-1, "main:/la/b/dir");
            assertFalse(desc.isPrimary());
            assertEquals("main:/a/b/dir", desc.getIndirection());
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }

    /**
     * Test rename behavior of an overlayed file withing a layer.
     */
    public void testFileRenameLayer()
    {
        try
        {
            // Make a set of directories.
            fService.createDirectory("main:/", "a");
            fService.createDirectory("main:/a", "b");
            fService.createDirectory("main:/a/b", "c");
            fService.createFile("main:/", "foo").close();
            fService.createSnapshot("main");
            // Make a layer.
            fService.createLayeredDirectory("main:/a", "main:/", "la");
            fService.createSnapshot("main");
            // Make a layered file.
            fService.createLayeredFile("main:/foo", "main:/la/b", "foo");
            fService.createSnapshot("main");
            AVMNodeDescriptor desc = fService.lookup(-1, "main:/la/b/foo");
            assertEquals("main:/foo", desc.getIndirection());
            // Now rename it. It should still point at the same place.
            fService.rename("main:/la/b", "foo", "main:/la", "foo");
            fService.createSnapshot("main");
            desc = fService.lookup(-1, "main:/la/foo");
            assertEquals("main:/foo", desc.getIndirection());
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * The random access.
     */
    public void testRandomAccess()
    {
        try
        {
            setupBasicTree();
            RandomAccessFile file = fService.getRandomAccess(1, "main:/a/b/c/foo", "r");
            byte [] buff = new byte[256];
            assertTrue(file.read(buff) >= 20);
            file.close();
            file = fService.getRandomAccess(-1, "main:/a/b/c/bar", "rw");
            for (int i = 0; i < 256; i++)
            {
                buff[i] = (byte)i;
            }
            file.write(buff);
            file.close();
            fService.createSnapshot("main");
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Test COW during long operations.
     */
    public void testCOWLongOps()
    {
        try
        {
            setupBasicTree();
            // Create a layer to a.
            fService.createLayeredDirectory("main:/a", "main:/d/e/f", "layer");
            // Create a layer to /d
            fService.createLayeredDirectory("main:/d", "main:/", "l2");
            // Force a copy on write on l2
            fService.createFile("main:/l2", "baz").close();
            // Force a copy on write on /d/e/f/layer/b/c
            fService.createFile("main:/d/e/f/layer/b/c", "fink").close();
            // Create /l2/e/f/layer/b/c/nottle
            fService.createFile("main:/l2/e/f/layer/b/c", "nottle").close();
            fService.createSnapshot("main");
            System.out.println(recursiveList("main", -1, true));
            assertFalse(fService.lookup(-1, "main:/d/e/f/layer/b/c").getId() ==
                        fService.lookup(-1, "main:/l2/e/f/layer/b/c").getId());
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Test new lookup methods.
     */
    public void testLookup()
    {
        try
        {
            setupBasicTree();
            AVMNodeDescriptor desc = fService.getAVMStoreRoot(-1, "main");
            assertNotNull(desc);
            System.out.println(desc.toString());
            AVMNodeDescriptor child = fService.lookup(desc, "a");
            assertNotNull(child);
            System.out.println(child.toString());
            child = fService.lookup(child, "b");
            assertNotNull(child);
            System.out.println(child.toString());
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Test version by date lookup.
     */
    public void testVersionByDate()
    {
        try
        {
            ArrayList<Long> times = new ArrayList<Long>();
            BulkLoader loader = new BulkLoader();
            loader.setAvmService(fService);
            loader.recursiveLoad("source/java/org/alfresco/repo/avm", "main:/");
            times.add(System.currentTimeMillis());
            assertEquals(1, fService.createSnapshot("main"));
            loader.recursiveLoad("source/java/org/alfresco/repo/action", "main:/");
            times.add(System.currentTimeMillis());
            assertEquals(2, fService.createSnapshot("main"));
            loader.recursiveLoad("source/java/org/alfresco/repo/audit", "main:/");
            times.add(System.currentTimeMillis());
            assertEquals(3, fService.createSnapshot("main"));
            assertEquals(1, fService.getAVMStoreVersions("main", null, new Date(times.get(0))).size());
            assertEquals(3, fService.getAVMStoreVersions("main", new Date(times.get(0)), null).size());
            assertEquals(2, fService.getAVMStoreVersions("main", new Date(times.get(1)),
                                                           new Date(System.currentTimeMillis())).size());
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Test AVMStore functions.
     */
    public void testAVMStore()
    {
        try
        {
            // First check that we get the right error when we try to create an
            // AVMStore that exists.
            try
            {
                fService.createAVMStore("main");
                fail();
            }
            catch (AVMExistsException ae)
            {
                // Do nothing.
            }
            // Now make sure getRepository() works.
            AVMStoreDescriptor desc = fService.getAVMStore("main");
            assertNotNull(desc);
            System.out.println(desc);
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Test opacity and manipulations.
     */
    public void testOpacity()
    {
        try
        {
            setupBasicTree();
            fService.createLayeredDirectory("main:/a", "main:/", "layer");
            fService.createSnapshot("main");
            fService.createFile("main:/layer/b/c", "baz").close();
            fService.createFile("main:/layer/b/c", "fig").close();
            fService.createSnapshot("main");
            Map<String, AVMNodeDescriptor> listing = fService.getDirectoryListing(-1, "main:/layer/b/c");
            assertEquals(4, listing.size());
            System.out.println(recursiveList("main", -1, true));
            // Setting the opacity of layer to true will make no difference to what we see through 
            // main:/layer/b/c.
            fService.setOpacity("main:/layer", true);
            fService.createSnapshot("main");
            assertTrue(fService.lookup(-1, "main:/layer").getOpacity());
            assertEquals(4, fService.getDirectoryListing(-1, "main:/layer/b/c").size());
            System.out.println(recursiveList("main", -1, true));
            // If main:/layer/b/c is opaque, however, we'll only see two items in it.
            fService.setOpacity("main:/layer", false);
            fService.setOpacity("main:/layer/b/c", true);
            fService.createSnapshot("main");
            assertFalse(fService.lookup(-1, "main:/layer").getOpacity());
            assertTrue(fService.lookup(-1, "main:/layer/b/c").getOpacity());
            assertEquals(2, fService.getDirectoryListing(-1, "main:/layer/b/c").size());
            System.out.println(recursiveList("main", -1, true));
            // Gratuitous test of retarget.
            fService.retargetLayeredDirectory("main:/layer", "main:/d");
            fService.setOpacity("main:/layer/b/c", false);
            fService.createSnapshot("main");
            assertFalse(fService.lookup(-1, "main:/layer/b/c").getOpacity());
            assertEquals(2, fService.getDirectoryListing(-1, "main:/layer/b/c").size());
            // This is just testing that opacity setting works on latent
            // layered directories.
            fService.setOpacity("main:/layer/e/f", true);
            System.out.println(recursiveList("main", -1, true));
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
    
    /**
     * Test common ancestor.
     */
    public void testCommonAncestor()
    {
        try
        {
            setupBasicTree();
            fService.createBranch(-1, "main:/a", "main:/", "branch");
            fService.createSnapshot("main");
            AVMNodeDescriptor ancestor = fService.lookup(-1, "main:/a/b/c/foo");
            fService.getFileOutputStream("main:/a/b/c/foo").close();
            fService.getFileOutputStream("main:/branch/b/c/foo").close();
            fService.createSnapshot("main");
            AVMNodeDescriptor main = fService.lookup(-1, "main:/a/b/c/foo");
            AVMNodeDescriptor branch = fService.lookup(-1, "main:/branch/b/c/foo");
            AVMNodeDescriptor ca = fService.getCommonAncestor(main, branch);
            assertEquals(ancestor, ca);
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail();
        }
    }
}
