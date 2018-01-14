package rdupes;

import java.util.Collections;
import java.util.List;

import hu.qgears.commons.UtilEvent;
import hu.qgears.commons.UtilListenableProperty;

abstract public class RDupesObject {
	public final UtilEvent<RDupesObject> changed=new UtilEvent<>();
	public final UtilListenableProperty<Boolean> hasCollision=new UtilListenableProperty<>(false);

	abstract public List<RDupesObject> getChildren();
	abstract public String getSimpleName();
	public List<RDupesObject> getCollisions() {
		return Collections.EMPTY_LIST;
	}
	abstract public String getFullName();
}
