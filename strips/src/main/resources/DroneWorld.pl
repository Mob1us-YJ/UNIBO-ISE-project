% DroneWorld.pl - Working STRIPS implementation for drone delivery with Horn Map

% ---------- 动态状态谓词声明 ----------
:- dynamic(at_drone/2).
:- dynamic(energy/2).
:- dynamic(at_package/2).
:- dynamic(holding/2).
:- dynamic(delivered/1).

% -------------------------
% Static domain facts - HORN MAP LAYOUT
% -------------------------

% Locations - Horn shaped map with multiple crossroads
location(warehouse1).
location(warehouse2).        % 新增第二个仓库
location(houseA).
location(houseB).
location(houseC).           % 新增房屋
location(houseD).           % 新增房屋
location(base).
location(crossroad1).       % 主要交叉路口
location(crossroad2).       % 东部交叉路口
location(crossroad3).       % 新增北部交叉路口
location(crossroad4).       % 新增西部交叉路口
location(junction_north).   % 北部连接点
location(junction_south).   % 南部连接点

% Mark base location for recharge
base(base).

% Drone definitions
drone(drone1).
drone(drone2).              % 新增第二个无人机
max_energy(drone1, 100).
max_energy(drone2, 120).    % 第二个无人机能量更大

% Packages
package(pkg1).
package(pkg2).
package(pkg3).              % 新增包裹
package(pkg4).              % 新增包裹

% Horn Map Road Network - 形成Horn形状的道路网络
% 主干道连接
connected(warehouse1, crossroad1).
connected(crossroad1, warehouse1).
distance(warehouse1, crossroad1, 5).
distance(crossroad1, warehouse1, 5).

connected(warehouse2, crossroad4).
connected(crossroad4, warehouse2).
distance(warehouse2, crossroad4, 4).
distance(crossroad4, warehouse2, 4).

% 交叉路口之间的连接 - 形成Horn的主体
connected(crossroad1, crossroad2).
connected(crossroad2, crossroad1).
distance(crossroad1, crossroad2, 8).
distance(crossroad2, crossroad1, 8).

connected(crossroad1, crossroad3).
connected(crossroad3, crossroad1).
distance(crossroad1, crossroad3, 6).
distance(crossroad3, crossroad1, 6).

connected(crossroad1, crossroad4).
connected(crossroad4, crossroad1).
distance(crossroad1, crossroad4, 7).
distance(crossroad4, crossroad1, 7).

connected(crossroad2, crossroad3).
connected(crossroad3, crossroad2).
distance(crossroad2, crossroad3, 9).
distance(crossroad3, crossroad2, 9).

% Horn的"角"部分连接
connected(crossroad3, junction_north).
connected(junction_north, crossroad3).
distance(crossroad3, junction_north, 5).
distance(junction_north, crossroad3, 5).

connected(crossroad4, junction_north).
connected(junction_north, crossroad4).
distance(crossroad4, junction_north, 6).
distance(junction_north, crossroad4, 6).

connected(crossroad2, junction_south).
connected(junction_south, crossroad2).
distance(crossroad2, junction_south, 4).
distance(junction_south, crossroad2, 4).

% 房屋连接
connected(crossroad1, houseA).
connected(houseA, crossroad1).
distance(crossroad1, houseA, 3).
distance(houseA, crossroad1, 3).

connected(crossroad2, houseB).
connected(houseB, crossroad2).
distance(crossroad2, houseB, 4).
distance(houseB, crossroad2, 4).

connected(crossroad3, houseC).
connected(houseC, crossroad3).
distance(crossroad3, houseC, 5).
distance(houseC, crossroad3, 5).

connected(crossroad4, houseD).
connected(houseD, crossroad4).
distance(crossroad4, houseD, 3).
distance(houseD, crossroad4, 3).

% 基地连接 - 位于Horn的中心
connected(crossroad1, base).
connected(base, crossroad1).
distance(crossroad1, base, 2).
distance(base, crossroad1, 2).

connected(crossroad2, base).
connected(base, crossroad2).
distance(crossroad2, base, 3).
distance(base, crossroad2, 3).

% 额外的长距离连接（可选，用于提供备选路径）
connected(junction_north, junction_south).
connected(junction_south, junction_north).
distance(junction_north, junction_south, 12).
distance(junction_south, junction_north, 12).

connected(houseA, houseB).
connected(houseB, houseA).
distance(houseA, houseB, 10).
distance(houseB, houseA, 10).

% Costs
pickup_cost(2).
deliver_cost(2).
recharge_rate(50).

% -------------------------
% 地图可视化辅助谓词（用于调试）
% -------------------------

safe_call(Goal) :-
    catch(Goal, error(instantiation_error,_), fail).

% 显示Horn地图的连接信息
show_horn_map :-
    write('Horn Map Network:'), nl,
    write('================='), nl,
    findall([From, To, Dist], (connected(From, To), distance(From, To, Dist)), Connections),
    show_connections(Connections).

show_connections([]).
show_connections([[From, To, Dist]|Rest]) :-
    format('~w -> ~w (distance: ~w)~n', [From, To, Dist]),
    show_connections(Rest).

% 查找从起点到终点的所有路径
find_all_paths(Start, End, Paths) :-
    findall(Path, find_path(Start, End, [Start], Path), Paths).

find_path(End, End, Visited, Visited).
find_path(Start, End, Visited, Path) :-
    connected(Start, Next),
    \+ member(Next, Visited),
    find_path(Next, End, [Next|Visited], Path).

% -------------------------
% 使用原始 Strips.pl 兼容格式的动作定义
% -------------------------

% Move action
action(move(D, From, To),
       if([at_drone(D, From), energy(D, E), E >= Cost]),
       +([at_drone(D, To), energy(D, NewE)]),
       -([at_drone(D, From), energy(D, E)]),
       where((connected(From, To), distance(From, To, Cost), NewE is E - Cost))) :-
    drone(D), location(From), location(To), From \= To.

% Pickup action
action(pickup(D, P, L),
       if([at_drone(D, L), at_package(P, L), energy(D, E), E >= Cost]),
       +([holding(D, P), energy(D, NewE)]),
       -([at_package(P, L), energy(D, E)]),
       where((pickup_cost(Cost), NewE is E - Cost))) :-
    drone(D), package(P), location(L).

% Drop action
action(drop(D, P, L),
       if([at_drone(D, L), holding(D, P), energy(D, E), E >= Cost]),
       +([at_package(P, L), energy(D, NewE)]),
       -([holding(D, P), energy(D, E)]),
       where((deliver_cost(Cost), NewE is E - Cost))) :-
    drone(D), package(P), location(L).

% Recharge action
action(recharge(D, BaseLoc),
       if([at_drone(D, BaseLoc), base(BaseLoc), energy(D, E)]),
       +([energy(D, NewE)]),
       -([energy(D, E)]),
       where((
           recharge_rate(R),
           max_energy(D, Max),
           TempE is E + R,
           ( TempE =< Max -> NewE = TempE ; NewE = Max )
       ))) :-
    drone(D), location(BaseLoc), base(BaseLoc).

% Full-recharge：一步把能量设为最大值
action(recharge_full(D),
       if([at_drone(D, Loc), base(Loc), energy(D, E), max_energy(D, Max), E < Max]),
       +([energy(D, Max)]),
       -([energy(D, E)]),
       where(true)) :-
    drone(D), location(Loc), base(Loc).

% -------------------------
% 简化的 STRIPS 实现（不依赖外部文件）
% -------------------------

% Main STRIPS predicate
strips(InitialState, Goals, Plan) :-
    (max_depth(MaxDepth) -> true ; MaxDepth = 15),
    retract_all_dynamic,
    assert_state(InitialState),
    find_plan(Goals, [], Plan, 0, MaxDepth),
    retract_all_dynamic.

% Assert initial state
assert_state([]).
assert_state([Fact|Rest]) :-
    assert(Fact),
    assert_state(Rest).

% Clean up dynamic facts - 修复语法错误
retract_all_dynamic :-
    retractall(at_drone(_, _)),
    retractall(energy(_, _)),
    retractall(at_package(_, _)),
    retractall(holding(_, _)),
    retractall(delivered(_)).

% Find plan
find_plan(Goals, Plan, Plan, _, _) :-
    goals_satisfied(Goals).

repeatable(Action) :-
    functor(Action, Name, _),
    ( Name == recharge ; Name == recharge_full ).

find_plan(Goals, CurrentPlan, FinalPlan, Depth, MaxDepth) :-
    Depth < MaxDepth,
    \+ goals_satisfied(Goals),
    select_unsatisfied_goal(Goals, Goal),
    find_action_for_goal(Goal, Action, Preconditions, AddList, DeleteList, Condition),
    ( repeatable(Action) -> true ; \+ member(Action, CurrentPlan) ),
    satisfy_preconditions(Preconditions),
    safe_call(Condition),
    apply_action_effects(AddList, DeleteList),
    NewDepth is Depth + 1,
    append(CurrentPlan, [Action], NewPlan),
    find_plan(Goals, NewPlan, FinalPlan, NewDepth, MaxDepth).

% Check if all goals are satisfied
goals_satisfied([]).
goals_satisfied([Goal|Rest]) :-
    call_safe(Goal),
    goals_satisfied(Rest).

% Safe goal calling
call_safe(Goal) :-
    catch(call(Goal), _, fail).

% Select an unsatisfied goal
select_unsatisfied_goal([Goal|_], Goal) :-
    \+ call_safe(Goal).
select_unsatisfied_goal([Goal|Rest], Selected) :-
    call_safe(Goal),
    select_unsatisfied_goal(Rest, Selected).

% Find action that can achieve a goal
find_action_for_goal(Goal, Action, Preconditions, AddList, DeleteList, Condition) :-
    action(Action, if(Preconditions), +(AddList), -(DeleteList), where(Condition)),
    member(Goal, AddList).

% Check if preconditions are satisfied
satisfy_preconditions([]).
satisfy_preconditions([Precond|Rest]) :-
    (call_safe(Precond) ; true),
    satisfy_preconditions(Rest).

% Apply action effects
apply_action_effects(AddList, DeleteList) :-
    retract_facts(DeleteList),
    assert_facts(AddList).

% Retract facts from DeleteList
retract_facts([]).
retract_facts([Fact|Rest]) :-
    (retract(Fact) -> true ; true),
    retract_facts(Rest).

% Assert facts from AddList
assert_facts([]).
assert_facts([Fact|Rest]) :-
    (call_safe(Fact) -> true ; assert(Fact)),
    assert_facts(Rest).

% -------------------------
% Horn地图世界初始化
% -------------------------

init_horn_world :-
    retract_all_dynamic,
    % 初始化两个无人机
    assert(at_drone(drone1, warehouse1)),
    assert(energy(drone1, 100)),
    assert(at_drone(drone2, warehouse2)),
    assert(energy(drone2, 120)),
    % 初始化包裹分布在不同仓库
    assert(at_package(pkg1, warehouse1)),
    assert(at_package(pkg2, warehouse1)),
    assert(at_package(pkg3, warehouse2)),
    assert(at_package(pkg4, warehouse2)).

% 原始世界初始化（保持兼容性）
init_world :-
    init_horn_world.

% -------------------------
% Horn地图测试示例
% -------------------------

% 测试Horn地图的复杂配送任务
test_horn_delivery :-
    init_horn_world,
    % 目标：将包裹分别配送到不同的房屋
    Goals = [at_package(pkg1, houseA),
             at_package(pkg2, houseB),
             at_package(pkg3, houseC),
             at_package(pkg4, houseD)],
    InitialState = [at_drone(drone1, warehouse1), energy(drone1, 100),
                   at_drone(drone2, warehouse2), energy(drone2, 120),
                   at_package(pkg1, warehouse1), at_package(pkg2, warehouse1),
                   at_package(pkg3, warehouse2), at_package(pkg4, warehouse2)],
    strips(InitialState, Goals, Plan),
    write('Horn Map Delivery Plan:'), nl,
    print_plan(Plan).

print_plan([]).
print_plan([Action|Rest]) :-
    write(Action), nl,
    print_plan(Rest).