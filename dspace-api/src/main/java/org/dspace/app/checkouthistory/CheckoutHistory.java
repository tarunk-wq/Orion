package org.dspace.app.checkouthistory;

import java.util.Date;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;

import jakarta.persistence.*;

@Entity
@Table(name = "checkout_history")
public class CheckoutHistory extends DSpaceObject {  

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eperson_id")
    private EPerson ePerson;
    
    private Date checkoutTime;

    private Date returnTime;

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

	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
	}

	public EPerson getePerson() {
		return ePerson;
	}

	public void setePerson(EPerson ePerson) {
		this.ePerson = ePerson;
	}

	public Date getCheckoutTime() {
		return checkoutTime;
	}

	public void setCheckoutTime(Date checkoutTime) {
		this.checkoutTime = checkoutTime;
	}

	public Date getReturnTime() {
		return returnTime;
	}

	public void setReturnTime(Date returnTime) {
		this.returnTime = returnTime;
	}

}
