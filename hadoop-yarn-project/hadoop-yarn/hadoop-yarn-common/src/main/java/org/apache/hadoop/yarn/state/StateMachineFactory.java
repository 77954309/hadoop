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

package org.apache.hadoop.yarn.state;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.apache.hadoop.classification.InterfaceAudience.Public;
import org.apache.hadoop.classification.InterfaceStability.Evolving;

/**
 * State machine topology.
 * This object is semantically immutable.  If you have a
 * StateMachineFactory there's no operation in the API that changes
 * its semantic properties.
 *
 * @param <OPERAND> The object type on which this state machine operates.
 * @param <STATE> The state of the entity.
 * @param <EVENTTYPE> The external eventType to be handled.
 * @param <EVENT> The event object.
 * 状态机工厂
 */
@Public
@Evolving
final public class StateMachineFactory
             <OPERAND, STATE extends Enum<STATE>,
              EVENTTYPE extends Enum<EVENTTYPE>, EVENT> {

  private final TransitionsListNode transitionsListNode;

  private Map<STATE, Map<EVENTTYPE,
    Transition<OPERAND, STATE, EVENTTYPE, EVENT>>> stateMachineTable;
  /**
   * 对象创建时，内部有限状态机的默认初始状态。
   */
  private STATE defaultInitialState;
  /**
   * 用于标记当前状态机是否需要优化性能，即构建状态拓扑表stateMachineTable
   */
  private final boolean optimized;

  /**
   * Constructor
   *
   * This is the only constructor in the API.
   * 公共构造器只有一个
   *
   */
  public StateMachineFactory(STATE defaultInitialState) {
    this.transitionsListNode = null;
    this.defaultInitialState = defaultInitialState;
    this.optimized = false;
    this.stateMachineTable = null;
  }
  
  private StateMachineFactory
      (StateMachineFactory<OPERAND, STATE, EVENTTYPE, EVENT> that,
       ApplicableTransition<OPERAND, STATE, EVENTTYPE, EVENT> t) {
    this.defaultInitialState = that.defaultInitialState;
    this.transitionsListNode 
        = new TransitionsListNode(t, that.transitionsListNode);
    this.optimized = false;
    this.stateMachineTable = null;
  }

  /**
   * 状态拓扑表，为了提高检索状态对应的过渡map冗余
   * 结构在optimized为真时，通过对transitionsListNode链表进行处理产生。
   *
   * installTopology中使用
   * @param that
   * @param optimized
   */
  private StateMachineFactory
      (StateMachineFactory<OPERAND, STATE, EVENTTYPE, EVENT> that,
       boolean optimized) {
    this.defaultInitialState = that.defaultInitialState;
    this.transitionsListNode = that.transitionsListNode;
    this.optimized = optimized;
    if (optimized) {
      makeStateMachineTable();
    } else {
      stateMachineTable = null;
    }
  }

  /**
   *YARN中提供了ApplicableTransition接口用于将SingleInternalArc和MultipleInternalArc添加到状态机的拓扑表中，
   * 提高在检索状态对应的过渡实现时的性能，ApplicableTransition的实现类为ApplicableSingleOrMultipleTransition类，
   * 其apply方法用于代理SingleInternalArc和MultipleInternalArc，将它们添加到状态拓扑表中
   *
   * @param <OPERAND>
   * @param <STATE>
   * @param <EVENTTYPE>
   * @param <EVENT>
   */
  private interface ApplicableTransition
             <OPERAND, STATE extends Enum<STATE>,
              EVENTTYPE extends Enum<EVENTTYPE>, EVENT> {
    void apply(StateMachineFactory<OPERAND, STATE, EVENTTYPE, EVENT> subject);
  }

  /**
   * 就是将状态机的一个个过渡的ApplicableTransition实现串联为一个列表，每个节点包含一个ApplicableTransition实现及指向下一个节点的引用
   */
  private class TransitionsListNode {
    final ApplicableTransition<OPERAND, STATE, EVENTTYPE, EVENT> transition;
    final TransitionsListNode next;

    TransitionsListNode
        (ApplicableTransition<OPERAND, STATE, EVENTTYPE, EVENT> transition,
        TransitionsListNode next) {
      this.transition = transition;
      this.next = next;
    }
  }

  static private class ApplicableSingleOrMultipleTransition
             <OPERAND, STATE extends Enum<STATE>,
              EVENTTYPE extends Enum<EVENTTYPE>, EVENT>
          implements ApplicableTransition<OPERAND, STATE, EVENTTYPE, EVENT> {
    final STATE preState;
    final EVENTTYPE eventType;
    final Transition<OPERAND, STATE, EVENTTYPE, EVENT> transition;

    ApplicableSingleOrMultipleTransition
        (STATE preState, EVENTTYPE eventType,
         Transition<OPERAND, STATE, EVENTTYPE, EVENT> transition) {
      this.preState = preState;
      this.eventType = eventType;
      this.transition = transition;
    }

    /**
     * 其apply方法用于代理SingleInternalArc和MultipleInternalArc，将它们添加到状态拓扑表中
     * @param subject
     */
    @Override
    public void apply
             (StateMachineFactory<OPERAND, STATE, EVENTTYPE, EVENT> subject) {
      Map<EVENTTYPE, Transition<OPERAND, STATE, EVENTTYPE, EVENT>> transitionMap
        = subject.stateMachineTable.get(preState);
      if (transitionMap == null) {
        // I use HashMap here because I would expect most EVENTTYPE's to not
        //  apply out of a particular state, so FSM sizes would be 
        //  quadratic if I use EnumMap's here as I do at the top level.
        transitionMap = new HashMap<EVENTTYPE,
          Transition<OPERAND, STATE, EVENTTYPE, EVENT>>();
        subject.stateMachineTable.put(preState, transitionMap);
      }
      transitionMap.put(eventType, transition);
    }
  }

  /**
   * @return a NEW StateMachineFactory just like {@code this} with the current
   *          transition added as a new legal transition.  This overload
   *          has no hook object.
   *
   *         Note that the returned StateMachineFactory is a distinct
   *         object.
   *
   *         This method is part of the API.
   *
   * @param preState pre-transition state
   * @param postState post-transition state
   * @param eventType stimulus for the transition
   */
  public StateMachineFactory
             <OPERAND, STATE, EVENTTYPE, EVENT>
          addTransition(STATE preState, STATE postState, EVENTTYPE eventType) {
    return addTransition(preState, postState, eventType, null);
  }

  /**
   * @return a NEW StateMachineFactory just like {@code this} with the current
   *          transition added as a new legal transition.  This overload
   *          has no hook object.
   *
   *
   *         Note that the returned StateMachineFactory is a distinct
   *         object.
   *
   *         This method is part of the API.
   *
   * @param preState pre-transition state
   * @param postState post-transition state
   * @param eventTypes List of stimuli for the transitions
   */
  public StateMachineFactory<OPERAND, STATE, EVENTTYPE, EVENT> addTransition(
      STATE preState, STATE postState, Set<EVENTTYPE> eventTypes) {
    return addTransition(preState, postState, eventTypes, null);
  }

  /**
   * @return a NEW StateMachineFactory just like {@code this} with the current
   *          transition added as a new legal transition
   *
   *         Note that the returned StateMachineFactory is a distinct
   *         object.
   *
   *         This method is part of the API.
   *
   * @param preState pre-transition state
   * @param postState post-transition state
   * @param eventTypes List of stimuli for the transitions
   * @param hook transition hook
   */
  public StateMachineFactory<OPERAND, STATE, EVENTTYPE, EVENT> addTransition(
      STATE preState, STATE postState, Set<EVENTTYPE> eventTypes,
      SingleArcTransition<OPERAND, EVENT> hook) {
    StateMachineFactory<OPERAND, STATE, EVENTTYPE, EVENT> factory = null;
    for (EVENTTYPE event : eventTypes) {
      if (factory == null) {
        factory = addTransition(preState, postState, event, hook);
      } else {
        factory = factory.addTransition(preState, postState, event, hook);
      }
    }
    return factory;
  }

  /**
   * @return a NEW StateMachineFactory just like {@code this} with the current
   *          transition added as a new legal transition
   *
   *         Note that the returned StateMachineFactory is a distinct object.
   *
   *         This method is part of the API.
   *
   * @param preState pre-transition state
   * @param postState post-transition state
   * @param eventType stimulus for the transition
   * @param hook transition hook
   * 用户添加各种状态转移
   *
   */
  public StateMachineFactory
             <OPERAND, STATE, EVENTTYPE, EVENT>
          addTransition(STATE preState, STATE postState,
                        EVENTTYPE eventType,
                        SingleArcTransition<OPERAND, EVENT> hook){
    return new StateMachineFactory<OPERAND, STATE, EVENTTYPE, EVENT>
        (this, new ApplicableSingleOrMultipleTransition<OPERAND, STATE, EVENTTYPE, EVENT>
           (preState, eventType, new SingleInternalArc(postState, hook)));
  }

  /**
   * @return a NEW StateMachineFactory just like {@code this} with the current
   *          transition added as a new legal transition
   *
   *         Note that the returned StateMachineFactory is a distinct object.
   *
   *         This method is part of the API.
   *
   * @param preState pre-transition state
   * @param postStates valid post-transition states
   * @param eventType stimulus for the transition
   * @param hook transition hook
   */
  public StateMachineFactory
             <OPERAND, STATE, EVENTTYPE, EVENT>
          addTransition(STATE preState, Set<STATE> postStates,
                        EVENTTYPE eventType,
                        MultipleArcTransition<OPERAND, EVENT, STATE> hook){
    return new StateMachineFactory<OPERAND, STATE, EVENTTYPE, EVENT>
        (this,
         new ApplicableSingleOrMultipleTransition<OPERAND, STATE, EVENTTYPE, EVENT>
           (preState, eventType, new MultipleInternalArc(postStates, hook)));
  }

  /**
   * @return a StateMachineFactory just like {@code this}, except that if
   *         you won't need any synchronization to build a state machine
   *
   *         Note that the returned StateMachineFactory is a distinct object.
   *
   *         This method is part of the API.
   *
   *         The only way you could distinguish the returned
   *         StateMachineFactory from {@code this} would be by
   *         measuring the performance of the derived 
   *         {@code StateMachine} you can get from it.
   *
   * Calling this is optional.  It doesn't change the semantics of the factory,
   *   if you call it then when you use the factory there is no synchronization.
   */
  public StateMachineFactory
             <OPERAND, STATE, EVENTTYPE, EVENT>
          installTopology() {
    return new StateMachineFactory<OPERAND, STATE, EVENTTYPE, EVENT>(this, true);
  }

  /**
   * Effect a transition due to the effecting stimulus.
   * @param state current state
   * @param eventType trigger to initiate the transition
   * @param cause causal eventType context
   * @return transitioned state
   */
  private STATE doTransition
           (OPERAND operand, STATE oldState, EVENTTYPE eventType, EVENT event)
      throws InvalidStateTransitonException {
    // We can assume that stateMachineTable is non-null because we call
    //  maybeMakeStateMachineTable() when we build an InnerStateMachine ,
    //  and this code only gets called from inside a working InnerStateMachine .
    Map<EVENTTYPE, Transition<OPERAND, STATE, EVENTTYPE, EVENT>> transitionMap
      = stateMachineTable.get(oldState);
    if (transitionMap != null) {
      Transition<OPERAND, STATE, EVENTTYPE, EVENT> transition
          = transitionMap.get(eventType);
      if (transition != null) {
        return transition.doTransition(operand, oldState, event, eventType);
      }
    }
    throw new InvalidStateTransitonException(oldState, eventType);
  }

  private synchronized void maybeMakeStateMachineTable() {
    if (stateMachineTable == null) {
      makeStateMachineTable();
    }
  }

  /**
   * transitionsListNode链表进行处理产生
   * 创建堆栈stack，用于将transitionsListNode链表中各个节点持有的ApplicableSingleOrMultipleTransition压入栈中；
   * 创建状态拓扑表stateMachineTable，并在此拓扑表中插入一个额外的默认初始状态defaultInitialState与null的映射；
   * 迭代访问transitionsListNode链表，并将各个节点持有的ApplicableSingleOrMultipleTransition压入栈中；
   * 依次弹出栈顶的ApplicableSingleOrMultipleTransition，并应用其apply方法，持续不断的构建状态拓扑表stateMachineTable。
   */
  private void makeStateMachineTable() {
    Stack<ApplicableTransition<OPERAND, STATE, EVENTTYPE, EVENT>> stack =
      new Stack<ApplicableTransition<OPERAND, STATE, EVENTTYPE, EVENT>>();

    Map<STATE, Map<EVENTTYPE, Transition<OPERAND, STATE, EVENTTYPE, EVENT>>>
      prototype = new HashMap<STATE, Map<EVENTTYPE, Transition<OPERAND, STATE, EVENTTYPE, EVENT>>>();

    prototype.put(defaultInitialState, null);

    // I use EnumMap here because it'll be faster and denser.  I would
    //  expect most of the states to have at least one transition.
    stateMachineTable
       = new EnumMap<STATE, Map<EVENTTYPE,
                           Transition<OPERAND, STATE, EVENTTYPE, EVENT>>>(prototype);

    for (TransitionsListNode cursor = transitionsListNode;
         cursor != null;
         cursor = cursor.next) {
      stack.push(cursor.transition);
    }

    while (!stack.isEmpty()) {
      stack.pop().apply(this);
    }
  }

  private interface Transition<OPERAND, STATE extends Enum<STATE>,
          EVENTTYPE extends Enum<EVENTTYPE>, EVENT> {
    STATE doTransition(OPERAND operand, STATE oldState,
                       EVENT event, EVENTTYPE eventType);
  }

  /**
   * 作为Transition接口的实现类，在代理SingleArcTransition的同时，负责状态变换
   */
  private class SingleInternalArc
                    implements Transition<OPERAND, STATE, EVENTTYPE, EVENT> {

    private STATE postState;
    private SingleArcTransition<OPERAND, EVENT> hook; // transition hook

    SingleInternalArc(STATE postState,
        SingleArcTransition<OPERAND, EVENT> hook) {
      this.postState = postState;
      this.hook = hook;
    }

    @Override
    public STATE doTransition(OPERAND operand, STATE oldState,
                              EVENT event, EVENTTYPE eventType) {
      if (hook != null) {
        hook.transition(operand, event);
      }
      return postState;
    }
  }

  /**
   * MultipleInternalArc也实现了Transition接口，并在代理MultipleArcTransition的转换行为的同时，负责状态变换。
   */
  private class MultipleInternalArc
              implements Transition<OPERAND, STATE, EVENTTYPE, EVENT>{

    // Fields
    private Set<STATE> validPostStates;
    private MultipleArcTransition<OPERAND, EVENT, STATE> hook;  // transition hook

    MultipleInternalArc(Set<STATE> postStates,
                   MultipleArcTransition<OPERAND, EVENT, STATE> hook) {
      this.validPostStates = postStates;
      this.hook = hook;
    }

    @Override
    public STATE doTransition(OPERAND operand, STATE oldState,
                              EVENT event, EVENTTYPE eventType)
        throws InvalidStateTransitonException {
      STATE postState = hook.transition(operand, event);

      if (!validPostStates.contains(postState)) {
        throw new InvalidStateTransitonException(oldState, eventType);
      }
      return postState;
    }
  }

  /* 
   * @return a {@link StateMachine} that starts in 
   *         {@code initialState} and whose {@link Transition} s are
   *         applied to {@code operand} .
   *
   *         This is part of the API.
   *
   * @param operand the object upon which the returned 
   *                {@link StateMachine} will operate.
   * @param initialState the state in which the returned 
   *                {@link StateMachine} will start.
   *                
   */
  public StateMachine<STATE, EVENTTYPE, EVENT>
        make(OPERAND operand, STATE initialState) {
    return new InternalStateMachine(operand, initialState);
  }

  /* 
   * @return a {@link StateMachine} that starts in the default initial
   *          state and whose {@link Transition} s are applied to
   *          {@code operand} . 
   *
   *         This is part of the API.
   *
   * @param operand the object upon which the returned 
   *                {@link StateMachine} will operate.
   *                
   */
  public StateMachine<STATE, EVENTTYPE, EVENT> make(OPERAND operand) {
    return new InternalStateMachine(operand, defaultInitialState);
  }

  private class InternalStateMachine
        implements StateMachine<STATE, EVENTTYPE, EVENT> {
    private final OPERAND operand;
    private STATE currentState;

    InternalStateMachine(OPERAND operand, STATE initialState) {
      this.operand = operand;
      this.currentState = initialState;
      if (!optimized) {
        maybeMakeStateMachineTable();
      }
    }

    @Override
    public synchronized STATE getCurrentState() {
      return currentState;
    }

    @Override
    public synchronized STATE doTransition(EVENTTYPE eventType, EVENT event)
         throws InvalidStateTransitonException  {
      currentState = StateMachineFactory.this.doTransition
          (operand, currentState, eventType, event);
      return currentState;
    }
  }

  /**
   * Generate a graph represents the state graph of this StateMachine
   * @param name graph name
   * @return Graph object generated
   */
  @SuppressWarnings("rawtypes")
  public Graph generateStateGraph(String name) {
    maybeMakeStateMachineTable();
    Graph g = new Graph(name);
    for (STATE startState : stateMachineTable.keySet()) {
      Map<EVENTTYPE, Transition<OPERAND, STATE, EVENTTYPE, EVENT>> transitions
          = stateMachineTable.get(startState);
      for (Entry<EVENTTYPE, Transition<OPERAND, STATE, EVENTTYPE, EVENT>> entry :
         transitions.entrySet()) {
        Transition<OPERAND, STATE, EVENTTYPE, EVENT> transition = entry.getValue();
        if (transition instanceof StateMachineFactory.SingleInternalArc) {
          StateMachineFactory.SingleInternalArc sa
              = (StateMachineFactory.SingleInternalArc) transition;
          Graph.Node fromNode = g.getNode(startState.toString());
          Graph.Node toNode = g.getNode(sa.postState.toString());
          fromNode.addEdge(toNode, entry.getKey().toString());
        } else if (transition instanceof StateMachineFactory.MultipleInternalArc) {
          StateMachineFactory.MultipleInternalArc ma
              = (StateMachineFactory.MultipleInternalArc) transition;
          Iterator iter = ma.validPostStates.iterator();
          while (iter.hasNext()) {
            Graph.Node fromNode = g.getNode(startState.toString());
            Graph.Node toNode = g.getNode(iter.next().toString());
            fromNode.addEdge(toNode, entry.getKey().toString());
          }
        }
      }
    }
    return g;
  }
}
