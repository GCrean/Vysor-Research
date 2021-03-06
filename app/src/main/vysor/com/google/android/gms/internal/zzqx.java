// 
// Decompiled by Procyon v0.5.30
// 

package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.IBinder;
import android.os.Binder;
import android.os.RemoteException;
import com.google.android.gms.common.api.Status;
import android.os.IInterface;

public interface zzqx extends IInterface
{
    void zzp(final Status p0) throws RemoteException;
    
    public abstract static class zza extends Binder implements zzqx
    {
        public zza() {
            this.attachInterface((IInterface)this, "com.google.android.gms.common.api.internal.IStatusCallback");
        }
        
        public static zzqx zzdp(final IBinder binder) {
            zzqx zzqx;
            if (binder == null) {
                zzqx = null;
            }
            else {
                final IInterface queryLocalInterface = binder.queryLocalInterface("com.google.android.gms.common.api.internal.IStatusCallback");
                if (queryLocalInterface != null && queryLocalInterface instanceof zzqx) {
                    zzqx = (zzqx)queryLocalInterface;
                }
                else {
                    zzqx = new zzqx.zza.zza(binder);
                }
            }
            return zzqx;
        }
        
        public IBinder asBinder() {
            return (IBinder)this;
        }
        
        public boolean onTransact(final int n, final Parcel parcel, final Parcel parcel2, final int n2) throws RemoteException {
            boolean onTransact = false;
            switch (n) {
                default: {
                    onTransact = super.onTransact(n, parcel, parcel2, n2);
                    break;
                }
                case 1598968902: {
                    parcel2.writeString("com.google.android.gms.common.api.internal.IStatusCallback");
                    onTransact = true;
                    break;
                }
                case 1: {
                    parcel.enforceInterface("com.google.android.gms.common.api.internal.IStatusCallback");
                    Status status;
                    if (parcel.readInt() != 0) {
                        status = (Status)Status.CREATOR.createFromParcel(parcel);
                    }
                    else {
                        status = null;
                    }
                    this.zzp(status);
                    onTransact = true;
                    break;
                }
            }
            return onTransact;
        }
        
        private static class zza implements zzqx
        {
            private IBinder zzajf;
            
            zza(final IBinder zzajf) {
                this.zzajf = zzajf;
            }
            
            public IBinder asBinder() {
                return this.zzajf;
            }
            
            @Override
            public void zzp(final Status status) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.google.android.gms.common.api.internal.IStatusCallback");
                    if (status != null) {
                        obtain.writeInt(1);
                        status.writeToParcel(obtain, 0);
                    }
                    else {
                        obtain.writeInt(0);
                    }
                    this.zzajf.transact(1, obtain, (Parcel)null, 1);
                }
                finally {
                    obtain.recycle();
                }
            }
        }
    }
}
