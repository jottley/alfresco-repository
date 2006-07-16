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

/**
 * Base class for Directories.
 * @author britt
 */
abstract class DirectoryNodeImpl extends AVMNodeImpl implements DirectoryNode
{
    /**
     * Default constructor.
     */
    protected DirectoryNodeImpl()
    {
    }
    
    /**
     * A pass through constructor. Called when a new concrete subclass
     * instance is created.
     * @param id
     * @param repo
     */
    protected DirectoryNodeImpl(long id, AVMStore repo)
    {
        super(id, repo);
    }
}
