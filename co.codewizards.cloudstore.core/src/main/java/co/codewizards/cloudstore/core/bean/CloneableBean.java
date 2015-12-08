package co.codewizards.cloudstore.core.bean;

public interface CloneableBean<P extends PropertyBase> extends Bean<P>, Cloneable  {

	Object clone();

}
