/**
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.apache.hadoop.yarn.server.resourcemanager.scheduler;

import java.io.IOException;

import org.apache.hadoop.classification.InterfaceAudience.LimitedPrivate;
import org.apache.hadoop.classification.InterfaceStability.Evolving;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.server.resourcemanager.RMContext;
import org.apache.hadoop.yarn.server.resourcemanager.recovery.Recoverable;

/**
 * This interface is the one implemented by the schedulers. It mainly extends 
 * {@link YarnScheduler}. 
 * 所有的资源调度器均应该实现接口
 * NODE_REMOVED：表示集群中移除了一个计算节点（可能是节点故障或者管理员主动移除），资源调度器收到该事件时需要从可分配资源总量中移除相应的资源量。
 * NODE_ADDED：表示集群中增加了一个计算节点，资源调度器收到该事件时需要将新增的资源量添加到可分配资源总量中。
 * APPLICATION_ADDED：表示ResourceManager收到一个新的Application。通常而言，资源管理器需要为每个Application维护一个独立的数据结构，以便于统一管理和资源分配。资源管理器需将该Application添加到相应的数据结构中。
 * APPLICATION_REMOVED：表示一个Application运行结束（可能成功或者失败），资源管理器需将该Application从相应的数据结构中清除。
 * CONTAINER_EXPIRED：当资源调度器将一个Container分配给某个Application-Master后，如果该ApplicationMaster在一定时间间隔内没有使用该Container，则资源调度器会对该Container进行（回收后）再分配。
 * NODE_UPDATE：ResourceManager收到NodeManager通过心跳机制汇报的信息后，会触发一个NODE_UDDATE事件，由于此时可能有新的Container得到释放，因此该事件会触发资源分配。也就是说，该事件是6个事件中最重要的事件，它会触发资源调度器最核心的资源分配机制。
 *
 *
 */
@LimitedPrivate("yarn")
@Evolving
public interface ResourceScheduler extends YarnScheduler, Recoverable {

  /**
   * Set RMContext for <code>ResourceScheduler</code>.
   * This method should be called immediately after instantiating
   * a scheduler once.
   * @param rmContext created by ResourceManager
   */
  void setRMContext(RMContext rmContext);

  /**
   * Re-initialize the <code>ResourceScheduler</code>.
   * @param conf configuration
   * @throws IOException
   *
   * 重新初始化ResourceScheduler,通常在ResourceManager初始化时调用
   * 包括主备ResourceManager切换
   *
   */
  void reinitialize(Configuration conf, RMContext rmContext) throws IOException;
}
