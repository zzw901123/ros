"""

Model trajectory generator

author: Atsushi Sakai(@Atsushi_twi)

"""

import math

import matplotlib.pyplot as plt
import numpy as np

import motion_model

# optimization parameter
max_iter = 100
h = np.array([0.5, 0.02, 0.02]).T  # parameter sampling distance
cost_th = 0.1

show_animation = True


def plot_arrow(x, y, yaw, length=1.0, width=0.5, fc="r", ec="k"):  # pragma: no cover
    """
    Plot arrow
    """
    plt.arrow(x, y, length * math.cos(yaw), length * math.sin(yaw),
              fc=fc, ec=ec, head_width=width, head_length=width)
    plt.plot(x, y)
    plt.plot(0, 0)


def calc_diff(target, x, y, yaw):
    """
    计算残差
    Parameters
    ----------
    target 目标状态，主要使用 x,y,yaw信息
    x 当前x坐标
    y 当前y坐标
    yaw 当前航向角yaw

    Returns
    -------
    残差
    """
    d = np.array([target.x - x[-1],
                  target.y - y[-1],
                  motion_model.pi_2_pi(target.yaw - yaw[-1])])

    return d


def calc_j(target, p, h, k0):
    """
    计算jacobian
    Parameters
    ----------
    target 目标状态
    p 当前参数
    h 对当前参数的微小扰动
    k0 初始速度

    Returns
    -------
    残差对当前参数p的雅克比
    """
    #第一个参数s进行扰动,s+e，得到扰动后的轨迹终态
    xp, yp, yawp = motion_model.generate_last_state(
        p[0, 0] + h[0], p[1, 0], p[2, 0], k0)
    #计算s+e扰动后的残差
    dp = calc_diff(target, [xp], [yp], [yawp])
    xn, yn, yawn = motion_model.generate_last_state(
        p[0, 0] - h[0], p[1, 0], p[2, 0], k0)
    # 计算s-e扰动后的残差
    dn = calc_diff(target, [xn], [yn], [yawn])
    # 得到参数s的偏导
    d1 = np.array((dp - dn) / (2.0 * h[0])).reshape(3, 1)
    print(d1)

    # 得到第二个参数的偏导
    xp, yp, yawp = motion_model.generate_last_state(
        p[0, 0], p[1, 0] + h[1], p[2, 0], k0)
    dp = calc_diff(target, [xp], [yp], [yawp])
    xn, yn, yawn = motion_model.generate_last_state(
        p[0, 0], p[1, 0] - h[1], p[2, 0], k0)
    dn = calc_diff(target, [xn], [yn], [yawn])
    d2 = np.array((dp - dn) / (2.0 * h[1])).reshape(3, 1)
    print(d2)

    #得到第三个参数的偏导
    xp, yp, yawp = motion_model.generate_last_state(
        p[0, 0], p[1, 0], p[2, 0] + h[2], k0)
    dp = calc_diff(target, [xp], [yp], [yawp])
    xn, yn, yawn = motion_model.generate_last_state(
        p[0, 0], p[1, 0], p[2, 0] - h[2], k0)
    dn = calc_diff(target, [xn], [yn], [yawn])
    d3 = np.array((dp - dn) / (2.0 * h[2])).reshape(3, 1)
    print(d3)

    # 组成对所有参数的偏导，即jacobian
    J = np.hstack((d1, d2, d3)) #np.hstack():在水平方向上平铺
    print(J)

    return J


def selection_learning_param(dp, p, k0, target):
    """
    选择牛顿迭代的步长
    Parameters
    ----------
    dp 牛顿迭代得到的delta_p
    p 当前的参数p
    k0 初始曲率
    target 目标状态

    Returns
    -------
    选择后较优的学习步长
    """

    mincost = float("inf")
    #牛顿迭代步长的取值范围
    mina = 1.0
    maxa = 2.0
    da = 0.5

    for a in np.arange(mina, maxa, da):
        # 按照步长a迭代参数，计算新的参数 tp
        tp = p + a * dp
        xc, yc, yawc = motion_model.generate_last_state(
            tp[0], tp[1], tp[2], k0)
        #计算新轨迹终态的残差
        dc = calc_diff(target, [xc], [yc], [yawc])
        #找使轨迹cost最小的 牛顿迭代步长
        cost = np.linalg.norm(dc)

        if cost <= mincost and a != 0.0:
            mina = a
            mincost = cost

    #  print(mincost, mina)
    #  input()

    return mina


def show_trajectory(target, xc, yc):  # pragma: no cover
    plt.clf()
    plot_arrow(target.x, target.y, target.yaw)
    plt.plot(xc, yc, "-r")
    plt.axis("equal")
    plt.grid(True)
    plt.pause(0.1)


def optimize_trajectory(target, k0, p):
    """
    给定目标状态target,在初始曲率k0,初始参数p[s,km,kf]的条件下，牛顿迭代得到最优参数，和最优参数下的轨迹
    Parameters
    ----------
    target 目标状态
    k0 初始曲率
    p 初始参数

    Returns
    -------
    轨迹，最优参数
    """
    for i in range(max_iter):
        
        # 按照初始的参数p生成一条轨迹
        # [xc, yc, yawc] 轨迹系列参数
        xc, yc, yawc = motion_model.generate_trajectory(p[0], p[1], p[2], k0)
        # 计算轨迹终态的残差
        dc = np.array(calc_diff(target, xc, yc, yawc)).reshape(3, 1)

        cost = np.linalg.norm(dc)
        if cost <= cost_th: # cost_th 收敛代价0.1
            print("path is ok cost is:" + str(cost))
            break
        
        # 计算残差对于当前参数p的jacobian
        J = calc_j(target, p, h, k0)
        try:
            # -jacobian取逆* 残差 得到参数p的更新量delta_p  控制参数变化量
            dp = - np.linalg.inv(J) @ dc
        except np.linalg.linalg.LinAlgError:
            print("cannot calc path LinAlgError")
            xc, yc, yawc, p = None, None, None, None
            break
        # 选择较优的迭代步长
        alpha = selection_learning_param(dp, p, k0, target)
        # 根据参数p的更新量和选择后的步长更新参数p
        p += alpha * np.array(dp)
        #  print(p.T)

        if show_animation:  # pragma: no cover
            show_trajectory(target, xc, yc)
    else:
        xc, yc, yawc, p = None, None, None, None
        print("cannot calc path")

    return xc, yc, yawc, p


def test_optimize_trajectory():  # pragma: no cover

    #  target = motion_model.State(x=5.0, y=2.0, yaw=np.deg2rad(00.0))
    target = motion_model.State(x=5.0, y=2.0, yaw=np.deg2rad(90.0))
    k0 = 0.0

    # 初始化参数
    init_p = np.array([6.0, 0.0, 0.0]).reshape(3, 1)

    x, y, yaw, p = optimize_trajectory(target, k0, init_p)

    if show_animation:
        show_trajectory(target, x, y)
        plot_arrow(target.x, target.y, target.yaw)
        plt.axis("equal")
        plt.grid(True)
        plt.show()


def main():  # pragma: no cover
    print(__file__ + " start!!")
    test_optimize_trajectory()


if __name__ == '__main__':
    main()
