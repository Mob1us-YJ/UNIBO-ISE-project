% Strips.pl
% A simple STRIPS backward planner implementation for tuProlog.
% Supports action definitions of the form:
%   action(
%       ActionTerm,
%       if [Preconditions...],
%       + [AddList...],
%       - [DelList...],
%       where(Condition)
%   ).
% Uses a dynamic max_depth(N) injected via KB for depth limit.

:- op(900, xfx, in).

% -------------------------
% subseteq/2: check that all elements of List1 are in State
subseteq([], _).
subseteq([X|Xs], State) :-
    member(X, State),
    subseteq(Xs, State).

% -------------------------
% Main entry point: strips(+InitState, +GoalList, -Plan)
% InitState: list of ground terms representing initial facts, e.g., [at_drone(drone1,warehouse1), energy(drone1,100)]
% GoalList: list of ground terms to achieve, e.g., [at_drone(drone1,crossroad1)]
% Plan: returned as a list of action terms, e.g., [move(drone1,warehouse1,crossroad1)]
% Requires that a dynamic fact max_depth(N) is present in KB; if absent, defaults to 10.
strips(InitState, GoalList, Plan) :-
    (   max_depth(Max) ->
        true
    ;   Max = 10
    ),
    strips_impl(InitState, GoalList, [], PlanRev, 0, Max),
    reverse(PlanRev, Plan).

% -------------------------
% strips_impl(+State, +GoalList, +PlanSoFar, -FinalPlan, +Depth, +MaxDepth)
% If current State already includes all GoalList, succeed.
strips_impl(State, GoalList, PlanSoFar, PlanSoFar, _, _) :-
    subseteq(GoalList, State),
    write("Reached goal "), write(GoalList), write(" in "), write(State), nl.

% Recursive case: Depth < MaxDepth, select a subgoal and an action to achieve it.
strips_impl(State, GoalList, PlanSoFar, Plan, Depth, Max) :-
    Depth < Max,
    select_goal(GoalList, State, SubGoal),
    write("Need to reach "), write(GoalList), write(" from "), write(State), nl,
    write("Attempting goal: "), write(SubGoal), nl,
    select_action_for(SubGoal, ActionTerm, PreList, AddList, DelList, Cond),
    \+ member(ActionTerm, PlanSoFar),  % Avoid reuse of same action in this path
    write("Choosing Action: "), write(ActionTerm), write(" -- not a bad action."), nl,
    write("Need to satisfy preconditions of "), write(ActionTerm), write(", that are: "), write(PreList), nl,
    satisfy_preconditions(PreList, State),
    apply(ActionTerm, State, NewState, Cond),
    NewDepth is Depth + 1,
    strips_impl(NewState, GoalList, [ActionTerm|PlanSoFar], Plan, NewDepth, Max).

% Failure case to prevent infinite backtracking beyond above clauses.
strips_impl(_, _, _, _, _, _) :-
    fail.

% -------------------------
% select_goal(+Goals, +State, -SubGoal)
% Pick a goal in Goals that is not yet satisfied in State.
select_goal(Goals, State, SubGoal) :-
    member(SubGoal, Goals),
    \+ member(SubGoal, State).

% -------------------------
% select_action_for(+SubGoal, -ActionTerm, -PreList, -AddList, -DelList, -Cond)
% Find an action whose AddList contains SubGoal.
select_action_for(SubGoal, ActionTerm, PreList, AddList, DelList, Cond) :-
    action(ActionTerm, if(PreList), +(AddList), -(DelList), where(Cond)),
    member(SubGoal, AddList).

% -------------------------
% satisfy_preconditions(+PreList, +State)
% For each precondition P:
%   - First try call(P): succeeds if static KB proves P or P is a comparison/is expression that holds.
%   - Otherwise try member(P, State): succeeds if P is in the current dynamic state list.
satisfy_preconditions([], _).
satisfy_preconditions([P|Ps], State) :-
    (   call(P)
    ;   member(P, State)
    ),
    satisfy_preconditions(Ps, State).

% -------------------------
% apply(+ActionTerm, +State, -NewState, +Cond)
% Apply an action to State to produce NewState:
%   1. Evaluate Cond (where clause). If Cond fails, action is not applicable.
%   2. Remove DelList facts from State.
%   3. Add AddList facts to State (avoiding duplicates).
apply(ActionTerm, State, NewState, Cond) :-
    % Retrieve AddList and DelList from action definition
    action(ActionTerm, if(_), +(AddList), -(DelList), where(CondTerm)),
    % Evaluate condition: either true or a Prolog term; use call/1
    (   CondTerm = true -> true
    ;   call(CondTerm)
    ),
    write("Simulating "), write(ActionTerm), nl,
    write("Transition: "), write(State), write(" - "), write(DelList), write(" + "), write(AddList),
    remove_list(DelList, State, State1),
    add_list(AddList, State1, NewState),
    write(" = "), write(NewState), nl.

% -------------------------
% remove_list(+DelList, +State, -State1)
remove_list([], State, State).
remove_list([X|Xs], State, State1) :-
    delete(State, X, Temp),
    remove_list(Xs, Temp, State1).

% -------------------------
% add_list(+AddList, +State, -State1)
add_list([], State, State).
add_list([X|Xs], State, State2) :-
    (   member(X, State) -> Temp = State ; Temp = [X|State] ),
    add_list(Xs, Temp, State2).
