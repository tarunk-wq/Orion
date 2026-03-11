package org.dspace.tempaccess;

import jakarta.persistence.*;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "temp_access")
public class TempAccess extends DSpaceObject {

	private static final long serialVersionUID = 1L;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "item")
	private Item item;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "eperson")
	private EPerson eperson;

	@Column(name = "start_date")
	private LocalDate startDate;

	@Column(name = "end_date")
	private LocalDate endDate;

	@Column(name = "department_uuid")
	private UUID departmentUuid;

	@Column(name = "isdeleted", nullable = false)
	private boolean deleted;

	public TempAccess() {
	}

	public TempAccess(EPerson eperson, Item item, LocalDate startDate, LocalDate endDate, UUID departmentUuid,
			boolean isdeleted) {
		this.eperson = eperson;
		this.item = item;
		this.startDate = startDate;
		this.endDate = endDate;
		this.departmentUuid = departmentUuid;
		this.deleted = isdeleted;
	}

	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
	}

	public EPerson getEperson() {
		return eperson;
	}

	public void setEperson(EPerson eperson) {
		this.eperson = eperson;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public UUID getDepartmentUuid() {
		return departmentUuid;
	}

	public void setDepartmentUuid(UUID departmentUuid) {
		this.departmentUuid = departmentUuid;
	}

	@Override
	public int getType() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}
}