"""

State lattice planner with model predictive trajectory generator

author: Atsushi Sakai (@Atsushi_twi)

- lookuptable.csv is generated with this script: https://github.com/AtsushiSakai/PythonRobotics/blob/master/PathPlanning/ModelPredictiveTrajectoryGenerator/lookuptable_generator.py

Ref:

- State Space Sampling of Feasible Motions for High-Performance Mobile Robot Navigation in Complex Environments http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.187.8210&rep=rep1&type=pdf

"""
import sys
import os
from matplotlib import pyplot as plt
import numpy as np
import math
import pandas as pd

sys.path.append(os.path.dirname(os.path.abspath(__file__))
                + "/../ModelPredictiveTrajectoryGenerator/")


try:
    import model_predictive_trajectory_generator as planner
    import motion_model
except:
    raise


table_path = os.path.dirname(os.path.abspath(__file__)) + "/lookuptable.csv"

show_animation = True


def search_nearest_one_from_lookuptable(tx, ty, tyaw, lookup_table):
    mind = float("inf")
    minid = -1

    for (i, table) in enumerate(lookup_table):

        dx = tx - table[0]
        dy = ty - table[1]
        dyaw = tyaw - table[2]
        d = math.sqrt(dx ** 2 + dy ** 2 + dyaw ** 2)
        if d <= mind:
            minid = i
            mind = d

    return lookup_table[minid]


def get_lookup_table():
    data = pd.read_csv(table_path)

    return np.array(data)

# 为target_states中采样得到的边界状态生成路径
def generate_path(target_states, k0):
    """
    k0是初始曲率，为target_states中的边界状态生成路径
    返回所有的路径和最优参数
    """
    # x, y, yaw, s, km, kf
    lookup_table = get_lookup_table()
    result = []

    for state in target_states:
        # 从lookup table中找到最佳的参考参数
        bestp = search_nearest_one_from_lookuptable(
            state[0], state[1], state[2], lookup_table)
        # 生成目标状态
        target = motion_model.State(x=state[0], y=state[1], yaw=state[2])
        # 把最佳参考作为初始化参数 
        # init_p [s,km,kf]
        # s: sqrt(state[0] ** 2 + state[1] ** 2
        # km: bestp[4]
        # kf: bestp[5]
        init_p = np.array(
            [math.sqrt(state[0] ** 2 + state[1] ** 2), bestp[4], bestp[5]]).reshape(3, 1) 
        # 优化路径，生成路径上点，及优化后参数p  路径
        x, y, yaw, p = planner.optimize_trajectory(target, k0, init_p)
        # 把结果加入result
        if x is not None:
            print("find good path")
            result.append(
                [x[-1], y[-1], yaw[-1], float(p[0]), float(p[1]), float(p[2])])

    print("finish path generation")
    return result

#np ,位置(position)采样的个数
#nh  ,航向(heading)采样的个数
#d ,初始点到终点的直线距离
#p_min, p_max , 位置采样的角度范围
#a_min, a_max , 航向角偏移量的角度范围

def calc_uniform_polar_states(nxy, nh, d, a_min, a_max, p_min, p_max):
    """
    calc uniform state

    :param nxy: number of position sampling 5
    :param nh: number of heading sampleing 3
    :param d: distance of terminal state 20
    :param a_min: position sampling min angle - np.deg2rad(45.0)
    :param a_max: position sampling max angle np.deg2rad(45.0)
    :param p_min: heading sampling min angle - np.deg2rad(45.0)
    :param p_max: heading sampling max angle np.deg2rad(45.0)
    :return: states list
    """
    # 均匀的采样角度，计算位置
    angle_samples = [i / (nxy - 1) for i in range(nxy)] #[0 1/4 1/2 3/4 1]
    states = sample_states(angle_samples, a_min, a_max, d, p_max, p_min, nh)

    return states


def calc_biased_polar_states(goal_angle, ns, nxy, nh, d, a_min, a_max, p_min, p_max):
    """
    calc biased state cost越小，采样越密集，cost越大，采样越稀疏

    :param goal_angle: goal orientation for biased sampling
    :param ns: number of biased sampling
    :param nxy: number of position sampling
    :param nh: number of heading sampleing
    :param d: distance of terminal state
    :param a_min: position sampling min angle
    :param a_max: position sampling max angle
    :param p_min: heading sampling min angle
    :param p_max: heading sampling max angle
    :return: states list
    """
    #位置角度按照ns个数均匀采样
    asi = [a_min + (a_max - a_min) * i / (ns - 1) for i in range(ns - 1)]
    # 计算cost，相当于对导航函数采样
    cnav = [math.pi - abs(i - goal_angle) for i in asi]
    # cost的总和
    cnav_sum = sum(cnav)
    cnav_max = max(cnav)

    # normalize normalize，生成新的分布，位置角度与终点角度相差小时cnav结果大；相反角度相差大时cnav结果小
    cnav = [(cnav_max - cnav[i]) / (cnav_max * ns - cnav_sum)
            for i in range(ns - 1)]
    # 对分布进行积分，这里角度相差小时，积分函数的变化缓慢；角度相差大时，积分函数变化快
    csumnav = np.cumsum(cnav)
    di = []
    li = 0
    # 对积分结果csumnav 按照nxy个数均匀采样，这样在角度相差小的区域，由于积分函数变化缓慢，采样的结果会更为密集
    for i in range(nxy):
        for ii in range(li, ns - 1):
            if ii / ns >= i / (nxy - 1):
                di.append(csumnav[ii])
                li = ii - 1
                break

    states = sample_states(di, a_min, a_max, d, p_max, p_min, nh)

    return states


def calc_lane_states(l_center, l_heading, l_width, v_width, d, nxy):
    """

    calc lane states

    :param l_center: lane lateral position 道路中心线的集合
    :param l_heading:  lane heading  道路的航向 
    :param l_width:  lane width      道路的宽度
    :param v_width: vehicle width    车的宽度
    :param d: longitudinal position  沿道路向前的弧长 
    :param nxy: sampling number 每条路线上横向偏移的采样数目
    :return: state list
    """
    xc = math.cos(l_heading) * d + math.sin(l_heading) * l_center
    yc = math.sin(l_heading) * d + math.cos(l_heading) * l_center

    states = []
    for i in range(nxy):
        delta = -0.5 * (l_width - v_width) + \
            (l_width - v_width) * i / (nxy - 1)
        xf = xc - delta * math.sin(l_heading)
        yf = yc + delta * math.cos(l_heading)
        yawf = l_heading
        states.append([xf, yf, yawf])

    return states

# angle_samples [0 0.25 0.5 0.75 1]
# a_min -pi/4
# a_max pi/4
# d 20
# p_min -pi/4
# p_max pi/4
# nh    3
def sample_states(angle_samples, a_min, a_max, d, p_max, p_min, nh):
    states = []
    for i in angle_samples: 
        #角度采样
        a = a_min + (a_max - a_min) * i
        #生成位置坐标
        for j in range(nh):
            xf = d * math.cos(a)
            yf = d * math.sin(a)
            #航向角
            if nh == 1:
                yawf = (p_max - p_min) / 2 + a
            else:
                yawf = p_min + (p_max - p_min) * j / (nh - 1) + a
            states.append([xf, yf, yawf])

    return states


def uniform_terminal_state_sampling_test1():
    k0 = 0.0
    nxy = 5
    nh = 3
    d = 20
    a_min = - np.deg2rad(45.0)
    a_max = np.deg2rad(45.0)
    p_min = - np.deg2rad(45.0)
    p_max = np.deg2rad(45.0)
    states = calc_uniform_polar_states(nxy, nh, d, a_min, a_max, p_min, p_max)
    result = generate_path(states, k0)

    for table in result:
        xc, yc, yawc = motion_model.generate_trajectory(
            table[3], table[4], table[5], k0)

        if show_animation:
            plt.plot(xc, yc, "-r")

    if show_animation:
        plt.grid(True)
        plt.axis("equal")
        plt.show()

    print("Done")


def uniform_terminal_state_sampling_test2():
    k0 = 0.1
    nxy = 6
    nh = 3
    d = 20
    a_min = - np.deg2rad(-10.0)
    a_max = np.deg2rad(45.0)
    p_min = - np.deg2rad(20.0)
    p_max = np.deg2rad(20.0)
    states = calc_uniform_polar_states(nxy, nh, d, a_min, a_max, p_min, p_max)
    result = generate_path(states, k0)

    for table in result:
        xc, yc, yawc = motion_model.generate_trajectory(
            table[3], table[4], table[5], k0)

        if show_animation:
            plt.plot(xc, yc, "-r")

    if show_animation:
        plt.grid(True)
        plt.axis("equal")
        plt.show()

    print("Done")


def biased_terminal_state_sampling_test1():
    k0 = 0.0
    nxy = 30
    nh = 2
    d = 20
    a_min = np.deg2rad(-45.0)
    a_max = np.deg2rad(45.0)
    p_min = - np.deg2rad(20.0)
    p_max = np.deg2rad(20.0)
    ns = 100
    goal_angle = np.deg2rad(0.0)
    states = calc_biased_polar_states(
        goal_angle, ns, nxy, nh, d, a_min, a_max, p_min, p_max)
    result = generate_path(states, k0)

    for table in result:
        xc, yc, yawc = motion_model.generate_trajectory(
            table[3], table[4], table[5], k0)
        if show_animation:
            plt.plot(xc, yc, "-r")

    if show_animation:
        plt.grid(True)
        plt.axis("equal")
        plt.show()


def biased_terminal_state_sampling_test2():
    k0 = 0.0
    nxy = 30
    nh = 1
    d = 20
    a_min = np.deg2rad(0.0)
    a_max = np.deg2rad(45.0)
    p_min = - np.deg2rad(20.0)
    p_max = np.deg2rad(20.0)
    ns = 100
    goal_angle = np.deg2rad(30.0)
    states = calc_biased_polar_states(
        goal_angle, ns, nxy, nh, d, a_min, a_max, p_min, p_max)
    result = generate_path(states, k0)

    for table in result:
        xc, yc, yawc = motion_model.generate_trajectory(
            table[3], table[4], table[5], k0)

        if show_animation:
            plt.plot(xc, yc, "-r")

    if show_animation:
        plt.grid(True)
        plt.axis("equal")
        plt.show()


def lane_state_sampling_test1():
    k0 = 0.0

    l_center = 10.0
    l_heading = np.deg2rad(90.0)
    l_width = 3.0
    v_width = 1.0
    d = 10
    nxy = 5
    # 车道线的采样
    states = calc_lane_states(l_center, l_heading, l_width, v_width, d, nxy)
    result = generate_path(states, k0) #输入参数:目标状态 起始斜率     起始点为(0，0) 相对于起始点 目标点坐标系而言  

    for table in result:
        xc, yc, yawc = motion_model.generate_trajectory(
            table[3], table[4], table[5], k0)

        if show_animation:
            plt.plot(xc, yc, "-r")

    if show_animation:
        plt.grid(True)
        plt.axis("equal")
        plt.show()


def main():
    print(os.path.abspath(__file__))
    #uniform_terminal_state_sampling_test1()
    #uniform_terminal_state_sampling_test2()
    #biased_terminal_state_sampling_test1()
    #biased_terminal_state_sampling_test2()
    lane_state_sampling_test1()


if __name__ == '__main__':
    main()
