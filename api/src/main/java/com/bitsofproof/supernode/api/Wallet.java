/*
 * Copyright 2013 Tamas Blummer tamas@bitsofproof.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bitsofproof.supernode.api;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class Wallet
{
	private final ExtendedKey master;
	private int nextKey;
	private List<Wallet> subs;

	private Wallet (ExtendedKey master, int nextKey)
	{
		this.master = master;
		this.nextKey = nextKey;
	}

	public static Wallet createWallet ()
	{
		SecureRandom random = new SecureRandom ();
		ECKeyPair master = ECKeyPair.createNew (true);
		byte[] chainCode = new byte[32];
		random.nextBytes (chainCode);
		ExtendedKey parent = new ExtendedKey (master, chainCode, 0);
		return new Wallet (parent, 0);
	}

	public static Wallet createWallet (ExtendedKey master)
	{
		return new Wallet (master, 0);
	}

	public Wallet createSubWallet (int sequence) throws ValidationException
	{
		if ( sequence > nextKey )
		{
			throw new ValidationException ("Subwallets must use consecutive sequences");
		}
		nextKey = Math.max (sequence + 1, nextKey);
		if ( subs == null )
		{
			subs = new ArrayList<Wallet> ();
		}
		Wallet sub = new Wallet (getKey (sequence), 0);
		subs.add (sub);
		return sub;
	}

	public ExtendedKey getKey (int sequence) throws ValidationException
	{
		if ( sequence > nextKey )
		{
			throw new ValidationException ("Sequence requested is higher generated before");
		}
		return KeyGenerator.generateKey (master, sequence);
	}

	public ExtendedKey generateNextKey () throws ValidationException
	{
		return KeyGenerator.generateKey (master, nextKey++);
	}

	public List<String> getAddresses (int addressFlag) throws ValidationException
	{
		ArrayList<String> addresses = new ArrayList<String> ();
		for ( int i = 0; i < nextKey; ++i )
		{
			ExtendedKey k = KeyGenerator.generateKey (master, i);
			addresses.add (AddressConverter.toSatoshiStyle (k.getKey ().getPublic (), addressFlag));
		}
		if ( subs != null )
		{
			for ( Wallet sub : subs )
			{
				addresses.addAll (sub.getAddresses (addressFlag));
			}
		}
		return addresses;
	}

	public ExtendedKey getMaster ()
	{
		return master;
	}

	public int getNextKeySequence ()
	{
		return nextKey;
	}
}