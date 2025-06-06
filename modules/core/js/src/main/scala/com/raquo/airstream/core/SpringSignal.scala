package com.raquo.airstream.core

import animus.{Animator, VectorArithmetic}
import com.raquo.airstream.common.SingleParentSignal

import scala.util.Try

class SpringSignal[A](
    override protected val parent: Signal[A],
    configureSpring: Animator[A] => Animator[A]
)(implicit
    vectorArithmetic: VectorArithmetic[A]
) extends Signal[A]
    with WritableSignal[A]
    with SingleParentSignal[A, A]:

  private var spring: Animator[A] = scala.compiletime.uninitialized
  var animationId: Int            = -1

  override def onStart(): Unit =
    super.onStart()
    animationId = AnimationManager.addAnimation(spring)

  override def onStop(): Unit =
    super.onStop()
    AnimationManager.removeAnimation(animationId)

  override protected def currentValueFromParent(): Try[A] =
    val value = parent.tryNow()
    value.foreach { a =>
      spring = configureSpring(Animator.make(a))
      spring.onChange = fireQuick
    }
    value

  override protected[airstream] val topoRank: Int = Protected.topoRank(parent) + 1

  def fireQuick(value: A): Unit =
    new Transaction(fireValue(value, _))

  override protected[airstream] def onTry(nextValue: Try[A], transaction: Transaction): Unit =
    nextValue.foreach { a =>
      spring.setTarget(a)
      if spring.isDone then
        spring.isDone = false
        AnimationManager.addAnimation(spring)
    }
