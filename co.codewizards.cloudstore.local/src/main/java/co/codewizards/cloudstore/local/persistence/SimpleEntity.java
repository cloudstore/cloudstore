//package co.codewizards.cloudstore.local.persistence;
//
//import javax.jdo.JDOHelper;
//import javax.jdo.annotations.IdGeneratorStrategy;
//import javax.jdo.annotations.IdentityType;
//import javax.jdo.annotations.NullValue;
//import javax.jdo.annotations.Persistent;
//import javax.jdo.annotations.PrimaryKey;
//
//@javax.jdo.annotations.PersistenceCapable(identityType=IdentityType.APPLICATION)
//public class SimpleEntity
//{
//
//	@PrimaryKey
//	@Persistent(valueStrategy=IdGeneratorStrategy.NATIVE)
//	private int id;
//
//	@Persistent(nullValue=NullValue.EXCEPTION)
//	private String key;
//
//	public SimpleEntity()
//	{
//	}
//
//	public SimpleEntity(String key)
//	{
//		this.key = key;
//	}
//
//	public int getId() {
//		return id;
//	}
//
//	public String getKey() {
//		return key;
//	}
//	public void setKey(String key) {
//		this.key = key;
//	}
//
//	@Override
//	public boolean equals(Object obj)
//	{
//		if (this == obj) {
//			return true;
//		}
//		if (obj == null) {
//			return false;
//		}
//
//		Object thisOid = JDOHelper.getObjectId(this);
//		if (thisOid == null) {
//			return false;
//		}
//
//		Object otherOid = JDOHelper.getObjectId(obj);
//		return thisOid.equals(otherOid);
//	}
//
//	@Override
//	public int hashCode()
//	{
//		Object thisOid = JDOHelper.getObjectId(this);
//		if (thisOid == null) {
//			return super.hashCode();
//		}
//		return thisOid.hashCode();
//	}
//
//	@Override
//	public String toString()
//	{
//		return String.format("%s[%s]", new Object[] { getClass().getSimpleName(), JDOHelper.getObjectId(this) });
//	}
//}